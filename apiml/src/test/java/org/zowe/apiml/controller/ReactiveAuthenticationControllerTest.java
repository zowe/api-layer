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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveAuthenticationControllerTest {

    private static final String BEARER = "Bearer ";
    @Mock private AuthenticationService authenticationService;
    @Mock private DiscoveryClient discoveryClient;
    @Mock private HttpUtils httpUtils;

    @Mock private SecurityContext securityContext;
    @Mock private TokenAuthentication tokenAuthentication;

    @InjectMocks
    private ReactiveAuthenticationController controller;

    private static ServiceInstance gatewayInstance() {
        return new DefaultServiceInstance(
            "localhost:gateway:10010", CoreService.GATEWAY.getServiceId(), "localhost", 10010, true);
    }

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
        when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(List.of(gatewayInstance()));
        when(authenticationService.invalidateJwtTokenGateway(eq(jwtToInvalidate), eq(false), anyList())).thenReturn(true);

        var result = controller.invalidateJwtToken(BEARER + jwtToInvalidate);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.OK.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void invalidateJwtToken_serviceUnavailable() {
        String jwtToInvalidate = "some.jwt.token";
        when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(List.of(gatewayInstance()));
        when(authenticationService.invalidateJwtTokenGateway(eq(jwtToInvalidate), eq(false), anyList())).thenReturn(false);

        var result = controller.invalidateJwtToken(BEARER + jwtToInvalidate);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.SERVICE_UNAVAILABLE.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void invalidateJwtToken_tokenNotValidException() {
        String jwtToInvalidate = "invalid.jwt.token";
        when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(List.of(gatewayInstance()));
        when(authenticationService.invalidateJwtTokenGateway(eq(jwtToInvalidate), eq(false), anyList()))
            .thenThrow(new TokenNotValidException("Token is not valid"));

        var result = controller.invalidateJwtToken(BEARER + jwtToInvalidate);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.BAD_REQUEST.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

}
