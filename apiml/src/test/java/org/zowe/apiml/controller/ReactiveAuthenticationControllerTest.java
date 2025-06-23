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

import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
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
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.HttpUtils;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveAuthenticationControllerTest {

    @Mock private AuthenticationService authenticationService;
    @Mock private PeerAwareInstanceRegistryImpl peerAwareInstanceRegistry;
    @Mock private HttpUtils httpUtils;

    @Mock private SecurityContext securityContext;
    @Mock private TokenAuthentication tokenAuthentication;

    @InjectMocks
    private ReactiveAuthenticationController controller;

    @Test
    void login_success() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login"));
        var jwtToken = "test-jwt-token";
        var username = "testUser";
        var mockCookie = ResponseCookie.from("apimlAuthenticationToken", jwtToken).build();

        when(tokenAuthentication.getCredentials()).thenReturn(jwtToken);
        when(tokenAuthentication.getName()).thenReturn(username);
        when(tokenAuthentication.isAuthenticated()).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(tokenAuthentication);
        when(httpUtils.createResponseCookie(jwtToken)).thenReturn(mockCookie);

        try (MockedStatic<ReactiveSecurityContextHolder> mockedContextHolder = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
            mockedContextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

            var result = controller.login(exchange, null);

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
            .expectNextMatches(responseEntity -> HttpStatus.BAD_REQUEST.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

}
