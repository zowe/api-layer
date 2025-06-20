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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.audit.RauditxService;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.zaas.controllers.AuthController;
import org.zowe.apiml.zaas.controllers.AuthController.RulesRequestModel;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactivePATControllerTest {

    @Mock private RauditxService rauditxService;
    @Mock private TokenAuthentication tokenAuthentication;
    @Mock private SecurityContext securityContext;
    @Mock private AccessTokenProvider tokenProvider;
    @Mock private MessageService messageService;
    @Mock private ObjectMapper mapper;

    @InjectMocks
    private ReactivePATController controller;

    @Test
    void generatePat_success() {
        var request =
            new ReactivePATController.AccessTokenRequest(3600, Set.of("scope1"));
        var username = "testUser";
        var pat = "generated-pat";

        var mockRauditBuilder = mock(RauditxService.RauditxBuilder.class);
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
        verify(mockRauditBuilder, times(1)).issue();
    }

    @Test
    void generatePat_failure() {
        var request =
            new ReactivePATController.AccessTokenRequest(3600, Set.of("scope1"));
        var username = "testUser";
        var exception = new RuntimeException("Token generation failed");

        var mockRauditBuilder = mock(RauditxService.RauditxBuilder.class);
        when(rauditxService.builder()).thenReturn(mockRauditBuilder);
        when(mockRauditBuilder.userId(anyString())).thenReturn(mockRauditBuilder);
        when(mockRauditBuilder.messageSegment(anyString())).thenReturn(mockRauditBuilder);
        when(mockRauditBuilder.alwaysLogSuccesses()).thenReturn(mockRauditBuilder);
        when(mockRauditBuilder.alwaysLogFailures()).thenReturn(mockRauditBuilder);

        when(tokenAuthentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(tokenAuthentication);
        when(tokenProvider.getToken(username, request.getValidity(), request.getScopes())).thenThrow(exception);

        try (var mockedContextHolder = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
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

    private AuthController.ValidateRequestModel createValidateRequestModel(String token, String serviceId) {
        AuthController.ValidateRequestModel model = new AuthController.ValidateRequestModel();
        model.setToken(token);
        model.setServiceId(serviceId);
        return model;
    }

    @Test
    void validateAccessToken_valid() {
        var requestModel = createValidateRequestModel("valid-token", "service1");
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
        var userId = "testUser";
        var timestamp = System.currentTimeMillis();
        var requestModel = new RulesRequestModel();
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
        var targetUserId = "userToRevoke";
        var timestamp = 12345L;
        var requestModel = new RulesRequestModel();
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
        var requestModel = new RulesRequestModel();
        requestModel.setUserId(null);

        var mockApiMessage = mock(Message.class);
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
        var targetServiceId = "serviceToRevoke";
        var timestamp = 12345L;
        var requestModel = new RulesRequestModel();
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
        var requestModel = new RulesRequestModel();
        requestModel.setServiceId(null);

        var mockApiMessage = mock(Message.class);
        var mockApiMessageView = mock(ApiMessageView.class);
        when(messageService.createMessage(anyString())).thenReturn(mockApiMessage);
        when(mockApiMessage.mapToView()).thenReturn(mockApiMessageView);

        var result = controller.revokeAccessTokensForScope(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.BAD_REQUEST.equals(responseEntity.getStatusCode()))
            .verifyComplete();
        verify(tokenProvider, never()).invalidateAllTokensForService(anyString(), anyLong());
        verify(messageService).createMessage("org.zowe.apiml.security.query.invalidRevokeRequestBody");
    }

}
