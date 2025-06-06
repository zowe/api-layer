/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.zowe.apiml.message.api.ApiMessage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.security.common.audit.RauditxService;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.HttpUtils;
import org.zowe.apiml.zaas.controllers.AuthController;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.JwtSecurity;
import org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderJWK;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import org.zowe.apiml.zaas.security.webfinger.WebFingerProvider;
import org.zowe.apiml.zaas.security.webfinger.WebFingerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveAuthenticationControllerTest {

    @Mock
    private JwtSecurity jwtSecurity;
    @Mock
    private ZosmfService zosmfService;
    @Mock
    private MessageService messageService;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private PeerAwareInstanceRegistryImpl peerAwareInstanceRegistry;
    @Mock
    private HttpUtils httpUtils;
    @Mock
    private AccessTokenProvider tokenProvider;
    @Mock
    private WebFingerProvider webFingerProvider;
    @Mock
    private RauditxService rauditxService;
    @Mock
    private OIDCProvider oidcProvider; // Nullable

    @Mock
    private SecurityContext securityContext;
    @Mock
    private TokenAuthentication tokenAuthentication;

    @InjectMocks
    private ReactiveAuthenticationController controller;

    private AuthController.ValidateRequestModel createValidateRequestModel(String token, String serviceId) {
        AuthController.ValidateRequestModel model = new AuthController.ValidateRequestModel();
        model.setToken(token);
        model.setServiceId(serviceId);
        return model;
    }

    @Test
    void login_success() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login"));
        String jwtToken = "test-jwt-token";
        String username = "testUser";
        ResponseCookie mockCookie = ResponseCookie.from("apimlAuthenticationToken", jwtToken).build();

        when(tokenAuthentication.getCredentials()).thenReturn(jwtToken);
        when(tokenAuthentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(tokenAuthentication);
        when(httpUtils.createResponseCookie(jwtToken)).thenReturn(mockCookie);

        try (MockedStatic<ReactiveSecurityContextHolder> mockedContextHolder = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
            mockedContextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

            Mono<ResponseEntity<Object>> result = controller.login(exchange);

            StepVerifier.create(result)
                .expectNextMatches(responseEntity -> {
                    assertEquals(mockCookie, exchange.getResponse().getCookies().getFirst("apimlAuthenticationToken"));
                    return HttpStatus.NO_CONTENT.equals(responseEntity.getStatusCode());
                })
                .verifyComplete();
        }
        verify(httpUtils).createResponseCookie(jwtToken);

    }

    @Test
    void generatePat_success() {
        ReactiveAuthenticationController.AccessTokenRequest request =
            new ReactiveAuthenticationController.AccessTokenRequest(3600, Set.of("scope1"));
        String username = "testUser";
        String pat = "generated-pat";

        RauditxService.RauditxBuilder mockRauditBuilder = mock(RauditxService.RauditxBuilder.class);
        when(rauditxService.builder()).thenReturn(mockRauditBuilder);
        when(mockRauditBuilder.userId(anyString())).thenReturn(mockRauditBuilder);
        when(mockRauditBuilder.messageSegment(anyString())).thenReturn(mockRauditBuilder);
        when(mockRauditBuilder.alwaysLogSuccesses()).thenReturn(mockRauditBuilder);
        when(mockRauditBuilder.alwaysLogFailures()).thenReturn(mockRauditBuilder);

        when(tokenAuthentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(tokenAuthentication);
        when(tokenProvider.getToken(username, request.getValidity(), request.getScopes())).thenReturn(pat);

        try (MockedStatic<ReactiveSecurityContextHolder> mockedContextHolder = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
            mockedContextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

            Mono<ResponseEntity<String>> result = controller.generatePat(request);

            StepVerifier.create(result)
                .expectNextMatches(responseEntity -> {
                    assertEquals(pat, responseEntity.getBody());
                    return HttpStatus.OK.equals(responseEntity.getStatusCode());
                })
                .verifyComplete();
        }
        verify(tokenProvider).getToken(username, request.getValidity(), request.getScopes());
        verify(mockRauditBuilder).success();
        verify(mockRauditBuilder, never()).failure();
        verify(mockRauditBuilder, never()).issue();
    }

    @Test
    void generatePat_failure() {
        ReactiveAuthenticationController.AccessTokenRequest request =
            new ReactiveAuthenticationController.AccessTokenRequest(3600, Set.of("scope1"));
        String username = "testUser";
        RuntimeException exception = new RuntimeException("Token generation failed");

        RauditxService.RauditxBuilder mockRauditBuilder = mock(RauditxService.RauditxBuilder.class);
        when(rauditxService.builder()).thenReturn(mockRauditBuilder);
        when(mockRauditBuilder.userId(anyString())).thenReturn(mockRauditBuilder);
        when(mockRauditBuilder.messageSegment(anyString())).thenReturn(mockRauditBuilder);
        when(mockRauditBuilder.alwaysLogSuccesses()).thenReturn(mockRauditBuilder);
        when(mockRauditBuilder.alwaysLogFailures()).thenReturn(mockRauditBuilder);

        when(tokenAuthentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(tokenAuthentication);
        when(tokenProvider.getToken(username, request.getValidity(), request.getScopes())).thenThrow(exception);

        try (MockedStatic<ReactiveSecurityContextHolder> mockedContextHolder = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
            mockedContextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

            Mono<ResponseEntity<String>> result = controller.generatePat(request);

            StepVerifier.create(result)
                .expectErrorMatches(exception::equals)
                .verify();
        }

        verify(tokenProvider).getToken(username, request.getValidity(), request.getScopes());
        verify(mockRauditBuilder).failure();
        verify(mockRauditBuilder).issue();
        verify(mockRauditBuilder, never()).success();
    }


    @Test
    void invalidateJwtToken_success() {
        String jwtToInvalidate = "some.jwt.token";
        String path = "/gateway/api/v1/auth/invalidate/" + jwtToInvalidate;
        MockServerHttpRequest mockRequest = MockServerHttpRequest.delete(path).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(mockRequest);

        Applications mockApplications = mock(Applications.class);
        Application mockApplication = mock(Application.class);
        when(peerAwareInstanceRegistry.getApplications()).thenReturn(mockApplications);
        when(mockApplications.getRegisteredApplications(CoreService.GATEWAY.getServiceId())).thenReturn(mockApplication);
        when(authenticationService.invalidateJwtTokenGateway(eq(jwtToInvalidate), eq(false), any(Application.class))).thenReturn(true);

        Mono<ResponseEntity<Void>> result = controller.invalidateJwtToken(exchange);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.OK.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void invalidateJwtToken_serviceUnavailable() {
        String jwtToInvalidate = "some.jwt.token";
        String path = "/gateway/api/v1/auth/invalidate/" + jwtToInvalidate;
        MockServerHttpRequest mockRequest = MockServerHttpRequest.delete(path).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(mockRequest);

        Applications mockApplications = mock(Applications.class);
        Application mockApplication = mock(Application.class);
        when(peerAwareInstanceRegistry.getApplications()).thenReturn(mockApplications);
        when(mockApplications.getRegisteredApplications(CoreService.GATEWAY.getServiceId())).thenReturn(mockApplication);
        when(authenticationService.invalidateJwtTokenGateway(eq(jwtToInvalidate), eq(false), any(Application.class))).thenReturn(false);

        Mono<ResponseEntity<Void>> result = controller.invalidateJwtToken(exchange);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.SERVICE_UNAVAILABLE.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void invalidateJwtToken_tokenNotValidException() {
        String jwtToInvalidate = "invalid.jwt.token";
        String path = "/gateway/api/v1/auth/invalidate/" + jwtToInvalidate;
        MockServerHttpRequest mockRequest = MockServerHttpRequest.delete(path).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(mockRequest);

        Applications mockApplications = mock(Applications.class);
        Application mockApplication = mock(Application.class);
        when(peerAwareInstanceRegistry.getApplications()).thenReturn(mockApplications);
        when(mockApplications.getRegisteredApplications(CoreService.GATEWAY.getServiceId())).thenReturn(mockApplication);
        when(authenticationService.invalidateJwtTokenGateway(eq(jwtToInvalidate), eq(false), any(Application.class)))
            .thenThrow(new TokenNotValidException("Token is not valid"));

        Mono<ResponseEntity<Void>> result = controller.invalidateJwtToken(exchange);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.SERVICE_UNAVAILABLE.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void validateAccessToken_valid() {
        AuthController.ValidateRequestModel requestModel = createValidateRequestModel("valid-token", "service1");
        when(tokenProvider.isValidForScopes("valid-token", "service1")).thenReturn(true);
        when(tokenProvider.isInvalidated("valid-token")).thenReturn(false);

        Mono<ResponseEntity<Object>> result = controller.validateAccessToken(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.NO_CONTENT.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void validateAccessToken_invalidScope() {
        AuthController.ValidateRequestModel requestModel = createValidateRequestModel("token-wrong-scope", "service1");
        when(tokenProvider.isValidForScopes("token-wrong-scope", "service1")).thenReturn(false);

        Mono<ResponseEntity<Object>> result = controller.validateAccessToken(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.UNAUTHORIZED.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void validateAccessToken_alreadyInvalidated() {
        AuthController.ValidateRequestModel requestModel = createValidateRequestModel("invalidated-token", "service1");
        when(tokenProvider.isValidForScopes("invalidated-token", "service1")).thenReturn(true);
        when(tokenProvider.isInvalidated("invalidated-token")).thenReturn(true);

        Mono<ResponseEntity<Object>> result = controller.validateAccessToken(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.UNAUTHORIZED.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void revokeAccessToken_success() throws IOException {
        Map<String, String> body = Collections.singletonMap("token", "token-to-revoke");
        Mono<Map<String, String>> bodyMono = Mono.just(body);

        when(tokenProvider.isInvalidated("token-to-revoke")).thenReturn(false);

        Mono<ResponseEntity<Object>> result = controller.revokeAccessToken(bodyMono);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.NO_CONTENT.equals(responseEntity.getStatusCode()))
            .verifyComplete();
        verify(tokenProvider).invalidateToken("token-to-revoke");
    }

    @Test
    void revokeAccessToken_missingToken() throws IOException {
        Map<String, String> body = Collections.singletonMap("token", ""); // or null
        Mono<Map<String, String>> bodyMono = Mono.just(body);

        Mono<ResponseEntity<Object>> result = controller.revokeAccessToken(bodyMono);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.BAD_REQUEST.equals(responseEntity.getStatusCode()))
            .verifyComplete();
        verify(tokenProvider, never()).invalidateToken(anyString());
    }

    @Test
    void revokeAccessToken_alreadyInvalidated() throws IOException {
        Map<String, String> body = Collections.singletonMap("token", "already-revoked-token");
        Mono<Map<String, String>> bodyMono = Mono.just(body);

        when(tokenProvider.isInvalidated("already-revoked-token")).thenReturn(true);

        Mono<ResponseEntity<Object>> result = controller.revokeAccessToken(bodyMono);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.UNAUTHORIZED.equals(responseEntity.getStatusCode()))
            .verifyComplete();
        verify(tokenProvider, never()).invalidateToken(anyString());
    }

    @Test
    void revokeAccessToken_ioExceptionOnInvalidate() throws IOException {
        Map<String, String> body = Collections.singletonMap("token", "token-cause-error");
        Mono<Map<String, String>> bodyMono = Mono.just(body);

        when(tokenProvider.isInvalidated("token-cause-error")).thenReturn(false);
        doThrow(new IOException("Disk full")).when(tokenProvider).invalidateToken("token-cause-error");

        Mono<ResponseEntity<Object>> result = controller.revokeAccessToken(bodyMono);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.SERVICE_UNAVAILABLE.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void revokeAllUserAccessTokens_success_withModel() {
        String userId = "testUser";
        long timestamp = System.currentTimeMillis();
        ReactiveAuthenticationController.RulesRequestModel requestModel = new ReactiveAuthenticationController.RulesRequestModel();
        requestModel.setTimestamp(timestamp);

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(userId);
        when(securityContext.getAuthentication()).thenReturn(mockAuth);

        try (MockedStatic<ReactiveSecurityContextHolder> mockedContextHolder = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
            mockedContextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

            Mono<ResponseEntity<Object>> result = controller.revokeAllUserAccessTokens(requestModel);

            StepVerifier.create(result)
                .expectNextMatches(responseEntity -> HttpStatus.NO_CONTENT.equals(responseEntity.getStatusCode()))
                .verifyComplete();
        }
        verify(tokenProvider).invalidateAllTokensForUser(userId, timestamp);
    }

    @Test
    void revokeAllUserAccessTokens_success_nullModel() {
        String userId = "testUser";
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(userId);
        when(securityContext.getAuthentication()).thenReturn(mockAuth);

        try (MockedStatic<ReactiveSecurityContextHolder> mockedContextHolder = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
            mockedContextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));
            Mono<ResponseEntity<Object>> result = controller.revokeAllUserAccessTokens(null);

            StepVerifier.create(result)
                .expectNextMatches(responseEntity -> HttpStatus.NO_CONTENT.equals(responseEntity.getStatusCode()))
                .verifyComplete();
        }
        verify(tokenProvider).invalidateAllTokensForUser(userId, 0L); // Timestamp defaults to 0
    }

    @Test
    void revokeAllUserAccessTokens_unauthorized() {
        try (MockedStatic<ReactiveSecurityContextHolder> mockedContextHolder = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
            mockedContextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.empty()); // Simulate no security context

            Mono<ResponseEntity<Object>> result = controller.revokeAllUserAccessTokens(null);

            StepVerifier.create(result)
                .expectNextMatches(responseEntity -> HttpStatus.UNAUTHORIZED.equals(responseEntity.getStatusCode()))
                .verifyComplete();
        }
        verify(tokenProvider, never()).invalidateAllTokensForUser(anyString(), anyLong());
    }


    @Test
    void revokeAccessTokensForUser_success() throws JsonProcessingException {
        String targetUserId = "userToRevoke";
        long timestamp = 12345L;
        ReactiveAuthenticationController.RulesRequestModel requestModel = new ReactiveAuthenticationController.RulesRequestModel();
        requestModel.setUserId(targetUserId);
        requestModel.setTimestamp(timestamp);

        Mono<ResponseEntity<String>> result = controller.revokeAccessTokensForUser(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.NO_CONTENT.equals(responseEntity.getStatusCode()))
            .verifyComplete();
        verify(tokenProvider).invalidateAllTokensForUser(targetUserId, timestamp);
    }

    @Test
    void revokeAccessTokensForUser_nullUserId() throws JsonProcessingException {
        ReactiveAuthenticationController.RulesRequestModel requestModel = new ReactiveAuthenticationController.RulesRequestModel();
        requestModel.setUserId(null);

        Message mockApiMessage = mock(Message.class);
        var mockApiMessageView = mock(ApiMessageView.class);
        when(messageService.createMessage(anyString())).thenReturn(mockApiMessage);
        when(mockApiMessage.mapToView()).thenReturn(mockApiMessageView);


        Mono<ResponseEntity<String>> result = controller.revokeAccessTokensForUser(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.BAD_REQUEST.equals(responseEntity.getStatusCode()))
            .verifyComplete();
        verify(tokenProvider, never()).invalidateAllTokensForUser(anyString(), anyLong());
        verify(messageService).createMessage("org.zowe.apiml.security.query.invalidRevokeRequestBody");
    }

    @Test
    void revokeAccessTokensForScope_success() throws JsonProcessingException {
        String targetServiceId = "serviceToRevoke";
        long timestamp = 12345L;
        ReactiveAuthenticationController.RulesRequestModel requestModel = new ReactiveAuthenticationController.RulesRequestModel();
        requestModel.setServiceId(targetServiceId);
        requestModel.setTimestamp(timestamp);

        Mono<ResponseEntity<String>> result = controller.revokeAccessTokensForScope(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.NO_CONTENT.equals(responseEntity.getStatusCode()))
            .verifyComplete();
        verify(tokenProvider).invalidateAllTokensForService(targetServiceId, timestamp);
    }

    @Test
    void revokeAccessTokensForScope_nullServiceId() throws JsonProcessingException {
        ReactiveAuthenticationController.RulesRequestModel requestModel = new ReactiveAuthenticationController.RulesRequestModel();
        requestModel.setServiceId(null);

        Message mockApiMessage = mock(Message.class);
        var mockApiMessageView = mock(ApiMessageView.class);
        when(messageService.createMessage(anyString())).thenReturn(mockApiMessage);
        when(mockApiMessage.mapToView()).thenReturn(mockApiMessageView);

        Mono<ResponseEntity<String>> result = controller.revokeAccessTokensForScope(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.BAD_REQUEST.equals(responseEntity.getStatusCode()))
            .verifyComplete();
        verify(tokenProvider, never()).invalidateAllTokensForService(anyString(), anyLong());
        verify(messageService).createMessage("org.zowe.apiml.security.query.invalidRevokeRequestBody");
    }


    @Test
    void getAllPublicKeys_zosmfProducer_withOidc() throws Exception {
        JWK zosmfJwk = new RSAKey.Builder((RSAPublicKey) generateKeyPair().getPublic()).keyID("zosmfKey").build();
        JWKSet zosmfKeySet = new JWKSet(zosmfJwk);
        JWK apimlJwk = new RSAKey.Builder((RSAPublicKey) generateKeyPair().getPublic()).keyID("apimlKey").build();
        JWK oidcJwk = new RSAKey.Builder((RSAPublicKey) generateKeyPair().getPublic()).keyID("oidcKey").build();
        JWKSet oidcKeySet = new JWKSet(oidcJwk);

        OIDCTokenProviderJWK mockOidcProviderJwk = mock(OIDCTokenProviderJWK.class);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
        when(zosmfService.getPublicKeys()).thenReturn(zosmfKeySet);
        when(jwtSecurity.getJwkPublicKey()).thenReturn(Optional.of(apimlJwk));
        ReactiveAuthenticationController testControllerWithOidc = new ReactiveAuthenticationController(
            jwtSecurity, zosmfService, messageService, authenticationService,
            peerAwareInstanceRegistry, httpUtils, tokenProvider, webFingerProvider,
            rauditxService, mockOidcProviderJwk
        );


        when(mockOidcProviderJwk.getJwkSet()).thenReturn(oidcKeySet);

        Mono<Map<String, Object>> result = testControllerWithOidc.getAllPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(jsonObject -> {
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                assertEquals(3, keys.size()); // zosmf, apiml, oidc
                return keys.stream().anyMatch(k -> "zosmfKey".equals(k.get("kid"))) &&
                    keys.stream().anyMatch(k -> "apimlKey".equals(k.get("kid"))) &&
                    keys.stream().anyMatch(k -> "oidcKey".equals(k.get("kid")));
            })
            .verifyComplete();
    }

    @Test
    void getAllPublicKeys_apimlProducer_noOidc() throws Exception {
        JWK apimlJwk = new RSAKey.Builder((RSAPublicKey) generateKeyPair().getPublic()).keyID("apimlKey").build();

        ReactiveAuthenticationController testControllerNoOidc = new ReactiveAuthenticationController(
            jwtSecurity, zosmfService, messageService, authenticationService,
            peerAwareInstanceRegistry, httpUtils, tokenProvider, webFingerProvider,
            rauditxService, null
        );

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        when(jwtSecurity.getJwkPublicKey()).thenReturn(Optional.of(apimlJwk));

        Mono<Map<String, Object>> result = testControllerNoOidc.getAllPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(jsonObject -> {
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                assertEquals(1, keys.size());
                return "apimlKey".equals(keys.get(0).get("kid"));
            })
            .verifyComplete();
        verify(zosmfService, never()).getPublicKeys();
    }


    @Test
    void getCurrentPublicKeys_apimlProducer() throws Exception {
        JWK apimlJwk = new RSAKey.Builder((RSAPublicKey) generateKeyPair().getPublic()).keyID("currentApimlKey").build();
        JWKSet apimlKeySet = new JWKSet(apimlJwk);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        when(jwtSecurity.getPublicKeyInSet()).thenReturn(apimlKeySet);

        Mono<Map<String, Object>> result = controller.getCurrentPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(jsonObject -> {
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                assertEquals(1, keys.size());
                return "currentApimlKey".equals(keys.get(0).get("kid"));
            })
            .verifyComplete();
    }

    @Test
    void getCurrentPublicKeys_zosmfProducer() throws Exception {
        JWK zosmfJwk = new RSAKey.Builder((RSAPublicKey) generateKeyPair().getPublic()).keyID("currentZosmfKey").build();
        JWKSet zosmfKeySet = new JWKSet(zosmfJwk);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
        when(zosmfService.getPublicKeys()).thenReturn(zosmfKeySet);

        Mono<Map<String, Object>> result = controller.getCurrentPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(jsonObject -> {
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                assertEquals(1, keys.size());
                return "currentZosmfKey".equals(keys.get(0).get("kid"));
            })
            .verifyComplete();
    }

    @Test
    void getCurrentPublicKeys_unknownProducer() {
        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.UNKNOWN); // Or any other not APIML/ZOSMF

        Mono<Map<String, Object>> result = controller.getCurrentPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(jsonObject -> {
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                return keys.isEmpty();
            })
            .verifyComplete();
    }


    @Test
    void getPublicKeyUsedForSigning_success() throws Exception {
        KeyPair keyPair = generateKeyPair();
        JWK jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic()).keyID("signingKey").build();
        JWKSet keySet = new JWKSet(jwk);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        when(jwtSecurity.getPublicKeyInSet()).thenReturn(keySet);

        Mono<ResponseEntity<Object>> result = controller.getPublicKeyUsedForSigning();

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                String pem = (String) responseEntity.getBody();
                return pem.startsWith("-----BEGIN PUBLIC KEY-----") && pem.endsWith("-----END PUBLIC KEY-----\n");
            })
            .verifyComplete();
    }

    @Test
    void getPublicKeyUsedForSigning_noKeyAvailable() {
        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.UNKNOWN); // results in empty list from getCurrentKey
        var mockApiMessage = mock(Message.class);
        when(messageService.createMessage("org.zowe.apiml.zaas.keys.unknownState")).thenReturn(mockApiMessage);


        Mono<ResponseEntity<Object>> result = controller.getPublicKeyUsedForSigning();

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.INTERNAL_SERVER_ERROR.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void givenMultipleKeys_thenReturnErrorWithCorrectMessage() throws Exception {
        KeyPair kp1 = generateKeyPair();
        KeyPair kp2 = generateKeyPair();
        JWK jwk1 = new RSAKey.Builder((RSAPublicKey) kp1.getPublic()).keyID("key1").build();
        JWK jwk2 = new RSAKey.Builder((RSAPublicKey) kp2.getPublic()).keyID("key2").build();
        JWKSet keySet = new JWKSet(List.of(jwk1, jwk2));

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        when(jwtSecurity.getPublicKeyInSet()).thenReturn(keySet);
        var mockApiMessage = mock(Message.class);
        when(messageService.createMessage(
            "org.zowe.apiml.zaas.keys.wrongAmount",
            2
        )).thenReturn(mockApiMessage);
        ApiMessage expectedApiMessage = new ApiMessage("org.zowe.apiml.zaas.keys.wrongAmount", MessageType.ERROR, "ZWEAG715E", "cnt", null, null);

        lenient().when(mockApiMessage.mapToApiMessage()).thenReturn(expectedApiMessage);


        Mono<ResponseEntity<Object>> result = controller.getPublicKeyUsedForSigning();

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
                assertNotNull(responseEntity.getBody());
                return ((ApiMessage) responseEntity.getBody()).getMessageNumber().equals("ZWEAG715E");
            })
            .verifyComplete();
    }

    @Test
    void getPublicKeyUsedForSigning_joseException() throws Exception {
        JWK mockJwk = mock(JWK.class);
        RSAKey mockRsaKey = mock(RSAKey.class);
        JWKSet keySet = new JWKSet(mockJwk);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        when(jwtSecurity.getPublicKeyInSet()).thenReturn(keySet);
        when(mockJwk.toRSAKey()).thenReturn(mockRsaKey);
        when(mockRsaKey.toPublicKey()).thenThrow(new JOSEException("Test JOSE Exception"));

        var mockApiMessage = mock(Message.class);
        when(messageService.createMessage("org.zowe.apiml.zaas.keys.unknown")).thenReturn(mockApiMessage);
        ApiMessage expectedApiMessage = new ApiMessage("org.zowe.apiml.zaas.keys.unknown", MessageType.ERROR, "ZWEAG717E", "cnt", null, null);
        lenient().when(mockApiMessage.mapToApiMessage()).thenReturn(expectedApiMessage);

        Mono<ResponseEntity<Object>> result = controller.getPublicKeyUsedForSigning();

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
                assertNotNull(responseEntity.getBody());
                return ((ApiMessage) responseEntity.getBody()).getMessageNumber().equals("ZWEAG717E");
            })
            .verifyComplete();
    }

    @Test
    void validateOIDCToken_valid() {
        ReactiveAuthenticationController testControllerWithOidc = new ReactiveAuthenticationController(
            jwtSecurity, zosmfService, messageService, authenticationService,
            peerAwareInstanceRegistry, httpUtils, tokenProvider, webFingerProvider,
            rauditxService, oidcProvider
        );
        AuthController.ValidateRequestModel requestModel = createValidateRequestModel("valid-oidc-token", null);
        when(oidcProvider.isValid("valid-oidc-token")).thenReturn(true);

        Mono<ResponseEntity<Void>> result = testControllerWithOidc.validateOIDCToken(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.NO_CONTENT.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void validateOIDCToken_invalid() {
        ReactiveAuthenticationController testControllerWithOidc = new ReactiveAuthenticationController(
            jwtSecurity, zosmfService, messageService, authenticationService,
            peerAwareInstanceRegistry, httpUtils, tokenProvider, webFingerProvider,
            rauditxService, oidcProvider
        );
        AuthController.ValidateRequestModel requestModel = createValidateRequestModel("invalid-oidc-token", null);
        when(oidcProvider.isValid("invalid-oidc-token")).thenReturn(false);

        Mono<ResponseEntity<Void>> result = testControllerWithOidc.validateOIDCToken(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.UNAUTHORIZED.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void validateOIDCToken_providerNull() {
        ReactiveAuthenticationController testControllerNoOidc = new ReactiveAuthenticationController(
            jwtSecurity, zosmfService, messageService, authenticationService,
            peerAwareInstanceRegistry, httpUtils, tokenProvider, webFingerProvider,
            rauditxService, null
        );
        AuthController.ValidateRequestModel requestModel = createValidateRequestModel("any-token", null);

        Mono<ResponseEntity<Void>> result = testControllerNoOidc.validateOIDCToken(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.UNAUTHORIZED.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void getWebFinger_enabled_success() throws IOException {
        String clientId = "testClient";
        WebFingerResponse mockResponse = new WebFingerResponse(); // Populate if necessary
        when(webFingerProvider.isEnabled()).thenReturn(true);
        when(webFingerProvider.getWebFingerConfig(clientId)).thenReturn(mockResponse);

        Mono<ResponseEntity<Object>> result = controller.getWebFinger(clientId);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                assertEquals(mockResponse, responseEntity.getBody());
                return true;
            })
            .verifyComplete();
    }

    @Test
    void getWebFinger_disabled() throws IOException {
        String clientId = "testClient";
        when(webFingerProvider.isEnabled()).thenReturn(false);

        Mono<ResponseEntity<Object>> result = controller.getWebFinger(clientId);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.NOT_FOUND.equals(responseEntity.getStatusCode()))
            .verifyComplete();
        verify(webFingerProvider, never()).getWebFingerConfig(anyString());
    }

    @Test
    void getWebFinger_enabled_ioException() throws IOException {
        String clientId = "testClient";
        when(webFingerProvider.isEnabled()).thenReturn(true);
        when(webFingerProvider.getWebFingerConfig(clientId)).thenThrow(new IOException("Config read error"));

        var mockApiMessage = mock(Message.class);
        var mockApiMessageView = mock(ApiMessageView.class);
        when(messageService.createMessage("org.zowe.apiml.security.oidc.invalidWebfingerConfiguration")).thenReturn(mockApiMessage);
        when(mockApiMessage.mapToView()).thenReturn(mockApiMessageView);


        Mono<ResponseEntity<Object>> result = controller.getWebFinger(clientId);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
                try {
                    String expectedBody = new ObjectMapper().writer().writeValueAsString(mockApiMessageView);
                    assertEquals(expectedBody, responseEntity.getBody());
                    return true;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            })
            .verifyComplete();
    }


    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }
}
