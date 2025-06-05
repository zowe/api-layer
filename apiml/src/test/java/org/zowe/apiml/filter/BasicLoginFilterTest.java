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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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
import reactor.util.context.Context;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicLoginFilterTest {

    @Mock
    private CompoundAuthProvider mockCompoundAuthProvider;
    @Mock
    private ObjectMapper mockObjectMapper;
    @Mock
    private FailedAuthenticationWebHandler mockFailedAuthenticationWebHandler;
    @Mock
    private WebFilterChain mockFilterChain;
    @Mock
    private Authentication mockAuthentication;

    private BasicLoginFilter basicLoginFilter;

    private MockedStatic<LoginFilter> staticLoginFilterMock;
    private MockedStatic<ReactiveSecurityContextHolder> staticSecurityContextHolderMock;

    @Captor
    private ArgumentCaptor<UsernamePasswordAuthenticationToken> authTokenCaptor;


    @BeforeEach
    void setUp() {
        basicLoginFilter = new BasicLoginFilter(mockCompoundAuthProvider, mockObjectMapper, mockFailedAuthenticationWebHandler);

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
        verifyNoInteractions(mockCompoundAuthProvider, mockObjectMapper, mockFailedAuthenticationWebHandler);
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
        String credentials = "testUser:testPassword";
        String basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        MockServerHttpRequest request = MockServerHttpRequest.post("/login")
            .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
            .build();
        ServerWebExchange exchange = createExchange(request);

        LoginRequest loginRequest = new LoginRequest("testUser", "testPassword".toCharArray());
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

        verify(mockFilterChain, times(2)).filter(exchange);
    }

    @Test
    void withValidBasicAuth_authenticationFailure_shouldDelegateToFailureHandler() {
        String credentials = "testUser:wrongPassword";
        String basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        MockServerHttpRequest request = MockServerHttpRequest.post("/login")
            .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
            .build();
        ServerWebExchange exchange = createExchange(request);

        LoginRequest loginRequest = new LoginRequest("testUser", "wrongPassword".toCharArray());
        staticLoginFilterMock.when(() -> LoginFilter.getCredentialFromAuthorizationHeader(Optional.of(basicAuthHeader)))
            .thenReturn(Optional.of(loginRequest));

        AuthenticationException authException = new BadCredentialsException("Invalid credentials");
        when(mockCompoundAuthProvider.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(authException);

        when(mockFailedAuthenticationWebHandler.onAuthenticationFailure(any(WebFilterExchange.class), eq(authException)))
            .thenReturn(Mono.empty());
        when(mockFilterChain.filter(exchange)).thenReturn(Mono.empty());
        StepVerifier.create(basicLoginFilter.filter(exchange, mockFilterChain))
            .verifyComplete();

        verify(mockFailedAuthenticationWebHandler).onAuthenticationFailure(any(WebFilterExchange.class), eq(authException));
    }

    @Test
    void withInvalidBasicAuthFormat_shouldDelegateToFailureHandler() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/login")
            .header(HttpHeaders.AUTHORIZATION, "Basic invalid-base64")
            .build();
        ServerWebExchange exchange = createExchange(request);

        staticLoginFilterMock.when(() -> LoginFilter.getCredentialFromAuthorizationHeader(any(Optional.class)))
            .thenThrow(new IllegalArgumentException("Simulated decode error"));

        AuthenticationException expectedException = new AuthenticationCredentialsNotFoundException("Invalid basic authentication header", new IllegalArgumentException("Simulated decode error"));

        when(mockFailedAuthenticationWebHandler.onAuthenticationFailure(any(WebFilterExchange.class), any(AuthenticationCredentialsNotFoundException.class)))
            .thenReturn(Mono.empty());
        when(mockFilterChain.filter(exchange)).thenReturn(Mono.empty());
        StepVerifier.create(basicLoginFilter.filter(exchange, mockFilterChain))
            .verifyComplete();

        ArgumentCaptor<AuthenticationCredentialsNotFoundException> exceptionCaptor = ArgumentCaptor.forClass(AuthenticationCredentialsNotFoundException.class);
        verify(mockFailedAuthenticationWebHandler).onAuthenticationFailure(any(WebFilterExchange.class), exceptionCaptor.capture());
        assertEquals(expectedException.getMessage(), exceptionCaptor.getValue().getMessage());
        assertTrue(exceptionCaptor.getValue().getCause() instanceof IllegalArgumentException);
    }


    @Test
    void withValidBodyAuth_authenticationSuccess_shouldSetSecurityContext() throws JsonProcessingException {
        LoginRequest loginRequestPojo = new LoginRequest("testUser", "testPassword".toCharArray());

        ObjectMapper realMapper = new ObjectMapper();
        basicLoginFilter = new BasicLoginFilter(mockCompoundAuthProvider, realMapper, mockFailedAuthenticationWebHandler);

        MockServerHttpRequest request = MockServerHttpRequest.post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(realMapper.writeValueAsString(loginRequestPojo));
        ServerWebExchange exchange = createExchange(request);


        staticLoginFilterMock.when(() -> LoginFilter.getCredentialFromAuthorizationHeader(any(HttpServletRequest.class)))
            .thenReturn(Optional.empty());

        when(mockCompoundAuthProvider.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mockAuthentication);

        Mono<Void> filteredMono = Mono.empty();
        when(mockFilterChain.filter(exchange)).thenReturn(filteredMono);
        staticSecurityContextHolderMock.when(() -> ReactiveSecurityContextHolder.withSecurityContext(any()))
            .thenAnswer(invocation -> Context.empty());

        StepVerifier.create(basicLoginFilter.filter(exchange, mockFilterChain))
            .verifyComplete();

        verify(mockCompoundAuthProvider).authenticate(authTokenCaptor.capture());
        assertEquals("testUser", authTokenCaptor.getValue().getName());
        assertEquals("testPassword", new String((char[]) authTokenCaptor.getValue().getCredentials()));

        verify(mockFilterChain).filter(exchange);
    }

    @Test
    void withValidBodyAuth_authenticationFailure_shouldDelegateToFailureHandler() throws JsonProcessingException {
        LoginRequest loginRequestPojo = new LoginRequest("user", "pass".toCharArray());
        ObjectMapper realMapper = new ObjectMapper();
        basicLoginFilter = new BasicLoginFilter(mockCompoundAuthProvider, realMapper, mockFailedAuthenticationWebHandler);

        MockServerHttpRequest request = MockServerHttpRequest.post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(realMapper.writeValueAsString(loginRequestPojo));
        ServerWebExchange exchange = createExchange(request);

        staticLoginFilterMock.when(() -> LoginFilter.getCredentialFromAuthorizationHeader(any(HttpServletRequest.class)))
            .thenReturn(Optional.empty());

        AuthenticationException authException = new BadCredentialsException("Failed login");
        when(mockCompoundAuthProvider.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(authException);

        when(mockFailedAuthenticationWebHandler.onAuthenticationFailure(any(WebFilterExchange.class), eq(authException)))
            .thenReturn(Mono.empty());
        when(mockFilterChain.filter(exchange)).thenReturn(Mono.empty());
        StepVerifier.create(basicLoginFilter.filter(exchange, mockFilterChain))
            .verifyComplete();

        verify(mockFailedAuthenticationWebHandler).onAuthenticationFailure(any(WebFilterExchange.class), eq(authException));
    }

    @Test
    void withInvalidJsonBody_shouldDelegateToFailureHandler() throws IOException {
        String malformedJsonBody = "{\"username\":\"testUser\", password:\"testPassword\""; // Malformed
        MockServerHttpRequest request = MockServerHttpRequest.post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(malformedJsonBody);
        ServerWebExchange exchange = createExchange(request);

        basicLoginFilter = new BasicLoginFilter(mockCompoundAuthProvider, mockObjectMapper, mockFailedAuthenticationWebHandler); // Ensure our mock ObjectMapper is used
        when(mockObjectMapper.readValue(anyString(), eq(LoginRequest.class))).thenThrow(new JsonMappingException("Simulated JSON parsing error"));


        staticLoginFilterMock.when(() -> LoginFilter.getCredentialFromAuthorizationHeader(any(HttpServletRequest.class)))
            .thenReturn(Optional.empty()); // No basic auth

        when(mockFailedAuthenticationWebHandler.onAuthenticationFailure(any(WebFilterExchange.class), any(AuthenticationCredentialsNotFoundException.class)))
            .thenReturn(Mono.empty());
        when(mockFilterChain.filter(exchange)).thenReturn(Mono.empty());
        StepVerifier.create(basicLoginFilter.filter(exchange, mockFilterChain))
            .verifyComplete();
//        RequestPath path = exchange.getRequest().getPath("/");

//        when(exchange.getRequest().getPath()).thenReturn(new DefaultRequestPath )
        ArgumentCaptor<AuthenticationCredentialsNotFoundException> exCaptor = ArgumentCaptor.forClass(AuthenticationCredentialsNotFoundException.class);
        verify(mockFailedAuthenticationWebHandler).onAuthenticationFailure(any(WebFilterExchange.class), exCaptor.capture());
        assertEquals("Login object has wrong format.", exCaptor.getValue().getMessage());
    }

    @Test
    void withJsonBodyMissingCredentials_shouldDelegateToFailureHandler() throws JsonProcessingException {
        LoginRequest incompleteLoginRequest = new LoginRequest(null, "testPassword".toCharArray());
        ObjectMapper realMapper = new ObjectMapper();
        basicLoginFilter = new BasicLoginFilter(mockCompoundAuthProvider, realMapper, mockFailedAuthenticationWebHandler);

        MockServerHttpRequest request = MockServerHttpRequest.post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(realMapper.writeValueAsString(incompleteLoginRequest));
        ServerWebExchange exchange = createExchange(request);

        staticLoginFilterMock.when(() -> LoginFilter.getCredentialFromAuthorizationHeader(any(HttpServletRequest.class)))
            .thenReturn(Optional.empty());

        when(mockFailedAuthenticationWebHandler.onAuthenticationFailure(any(WebFilterExchange.class), any(AuthenticationCredentialsNotFoundException.class)))
            .thenReturn(Mono.empty());
        when(mockFilterChain.filter(exchange)).thenReturn(Mono.empty());
        StepVerifier.create(basicLoginFilter.filter(exchange, mockFilterChain))
            .verifyComplete();

        ArgumentCaptor<AuthenticationCredentialsNotFoundException> exCaptor = ArgumentCaptor.forClass(AuthenticationCredentialsNotFoundException.class);
        verify(mockFailedAuthenticationWebHandler).onAuthenticationFailure(any(WebFilterExchange.class), exCaptor.capture());
        assertEquals("Login object has wrong format.", exCaptor.getValue().getMessage());
    }

    @Test
    void whenPathIsNotAuthLogin_shouldSkipBodyParsing() throws JsonProcessingException {
        LoginRequest loginRequestPojo = new LoginRequest("user", "pass".toCharArray());
        ObjectMapper realMapper = new ObjectMapper();

        MockServerHttpRequest request = MockServerHttpRequest.post("/some/other/path")
            .contentType(MediaType.APPLICATION_JSON)
            .body(realMapper.writeValueAsString(loginRequestPojo));
        ServerWebExchange exchange = createExchange(request);

        staticLoginFilterMock.when(() -> LoginFilter.getCredentialFromAuthorizationHeader(any(HttpServletRequest.class)))
            .thenReturn(Optional.empty());

        when(mockFilterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(basicLoginFilter.filter(exchange, mockFilterChain))
            .verifyComplete();

        verify(mockCompoundAuthProvider, never()).authenticate(any());
        verify(mockFilterChain).filter(exchange);
    }

}
