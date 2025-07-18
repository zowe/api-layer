/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.handler.FailedAuthenticationWebHandler;
import org.zowe.apiml.security.common.login.LoginFilter;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasicLoginFilterTest {

    @Mock private CompoundAuthProvider mockCompoundAuthProvider;
    @Mock private FailedAuthenticationWebHandler mockFailedAuthenticationWebHandler;
    @Mock private WebFilterChain mockFilterChain;
    @Mock private Authentication mockAuthentication;

    private BasicLoginFilter basicLoginFilter;

    private MockedStatic<LoginFilter> staticLoginFilterMock;
    private MockedStatic<ReactiveSecurityContextHolder> staticSecurityContextHolderMock;

    @Captor
    private ArgumentCaptor<UsernamePasswordAuthenticationToken> authTokenCaptor;

    @BeforeEach
    void setUp() {
        basicLoginFilter = new BasicLoginFilter(mockCompoundAuthProvider, mockFailedAuthenticationWebHandler);

        staticLoginFilterMock = Mockito.mockStatic(LoginFilter.class);
        staticSecurityContextHolderMock = Mockito.mockStatic(ReactiveSecurityContextHolder.class);

        lenient().when(mockCompoundAuthProvider.supports(any())).thenReturn(true); // Important for ProviderManager
        lenient().when(mockAuthentication.getName()).thenReturn("testUser");
    }

    @AfterEach
    void tearDown() {
        staticLoginFilterMock.close();
        staticSecurityContextHolderMock.close();
    }

    private ServerWebExchange createExchange(MockServerHttpRequest request) {
        return MockServerWebExchange.from(request);
    }


    @Test
    void whenBearerTokenPresent_shouldSkipFilterAndProceed() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/login")
            .header(HttpHeaders.AUTHORIZATION, "Bearer someTokenValue")
            .build();
        ServerWebExchange exchange = createExchange(request);

        when(mockFilterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(basicLoginFilter.filter(exchange, mockFilterChain))
            .verifyComplete();

        verify(mockFilterChain).filter(exchange);
        verifyNoInteractions(mockCompoundAuthProvider, mockFailedAuthenticationWebHandler);
        staticLoginFilterMock.verifyNoInteractions();
    }

    @Test
    void whenNoCredentialsProvided_shouldSkipFilterAndProceed() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/login").build();
        ServerWebExchange exchange = createExchange(request); // No auth header, empty body

        staticLoginFilterMock.when(() -> LoginFilter.getCredentialFromAuthorizationHeader(any(HttpServletRequest.class)))
            .thenReturn(Optional.empty()); // No basic auth
        when(mockFilterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(basicLoginFilter.filter(exchange, mockFilterChain))
            .verifyComplete();

        verify(mockFilterChain).filter(exchange);
        verifyNoInteractions(mockCompoundAuthProvider);
    }

    @Test
    void withValidBasicAuth_authenticationSuccess_andReachFinalChain() {
        var credentials = "testUser:testPassword";
        var basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        var request = MockServerHttpRequest.post("/login")
            .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
            .build();
        var exchange = createExchange(request);

        var loginRequest = new LoginRequest("testUser", "testPassword".toCharArray());
        staticLoginFilterMock.when(() -> LoginFilter.getCredentialFromAuthorizationHeader(Optional.of(basicAuthHeader)))
            .thenReturn(Optional.of(loginRequest));
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockCompoundAuthProvider.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mockAuthentication);

        Mono<Void> filteredMono = Mono.empty();

        when(mockFilterChain.filter(exchange)).thenReturn(filteredMono);


        StepVerifier.create(basicLoginFilter.filter(exchange, mockFilterChain))
            .verifyComplete();

        verify(mockCompoundAuthProvider).authenticate(authTokenCaptor.capture());
        assertEquals("testUser", authTokenCaptor.getValue().getName());
        assertEquals("testPassword", new String((char[]) authTokenCaptor.getValue().getCredentials()));

        verify(mockFilterChain, times(1)).filter(exchange);
    }

    @Test
    void withValidBasicAuth_authenticationFailure_shouldDelegateToFailureHandler() {
        var credentials = "testUser:wrongPassword";
        var basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        var request = MockServerHttpRequest.post("/login")
            .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
            .build();
        var exchange = createExchange(request);

        var loginRequest = new LoginRequest("testUser", "wrongPassword".toCharArray());
        staticLoginFilterMock.when(() -> LoginFilter.getCredentialFromAuthorizationHeader(Optional.of(basicAuthHeader)))
            .thenReturn(Optional.of(loginRequest));

        AuthenticationException authException = new BadCredentialsException("Invalid credentials");
        when(mockCompoundAuthProvider.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(authException);

        when(mockFailedAuthenticationWebHandler.onAuthenticationFailure(any(WebFilterExchange.class), eq(authException)))
            .thenReturn(Mono.empty());

        StepVerifier.create(basicLoginFilter.filter(exchange, mockFilterChain))
            .verifyComplete();

        verify(mockFailedAuthenticationWebHandler).onAuthenticationFailure(any(WebFilterExchange.class), eq(authException));
    }

    @SuppressWarnings("unchecked")
    @Test
    void withInvalidBasicAuthFormat_shouldDelegateToFailureHandler() {
        var request = MockServerHttpRequest.post("/login")
            .header(HttpHeaders.AUTHORIZATION, "Basic invalid-base64")
            .build();
        var exchange = createExchange(request);

        staticLoginFilterMock.when(() -> LoginFilter.getCredentialFromAuthorizationHeader(any(Optional.class)))
            .thenThrow(new IllegalArgumentException("Simulated decode error"));

        AuthenticationException expectedException = new AuthenticationCredentialsNotFoundException("Invalid basic authentication header", new IllegalArgumentException("Simulated decode error"));

        when(mockFailedAuthenticationWebHandler.onAuthenticationFailure(any(WebFilterExchange.class), any(AuthenticationCredentialsNotFoundException.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(basicLoginFilter.filter(exchange, mockFilterChain))
            .verifyComplete();

        ArgumentCaptor<AuthenticationCredentialsNotFoundException> exceptionCaptor = ArgumentCaptor.forClass(AuthenticationCredentialsNotFoundException.class);
        verify(mockFailedAuthenticationWebHandler).onAuthenticationFailure(any(WebFilterExchange.class), exceptionCaptor.capture());
        assertEquals(expectedException.getMessage(), exceptionCaptor.getValue().getMessage());
        assertInstanceOf(IllegalArgumentException.class, exceptionCaptor.getValue().getCause());
    }


    @Test
    void whenPathIsNotAuthLogin_shouldSkipBodyParsing() throws JsonProcessingException {
        var loginRequestPojo = new LoginRequest("user", "pass".toCharArray());
        var realMapper = new ObjectMapper();

        var request = MockServerHttpRequest.post("/some/other/path")
            .contentType(MediaType.APPLICATION_JSON)
            .body(realMapper.writeValueAsString(loginRequestPojo));
        var exchange = createExchange(request);

        staticLoginFilterMock.when(() -> LoginFilter.getCredentialFromAuthorizationHeader(any(HttpServletRequest.class)))
            .thenReturn(Optional.empty());

        when(mockFilterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(basicLoginFilter.filter(exchange, mockFilterChain))
            .verifyComplete();

        verify(mockCompoundAuthProvider, never()).authenticate(any());
        verify(mockFilterChain).filter(exchange);
    }

}
