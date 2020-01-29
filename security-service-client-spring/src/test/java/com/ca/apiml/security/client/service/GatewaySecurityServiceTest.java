/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.client.service;

import com.ca.apiml.security.client.handler.RestResponseHandler;
import com.ca.apiml.security.common.config.AuthConfigurationProperties;
import com.ca.apiml.security.common.token.QueryResponse;
import com.ca.apiml.security.common.token.TokenNotValidException;
import com.ca.mfaas.product.gateway.GatewayClient;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GatewaySecurityServiceTest {
    private static final String USERNAME = "user";
    private static final String PASSWORD = "pass";
    private static final String TOKEN = "token";
    private static final String GATEWAY_SCHEME = "https";
    private static final String GATEWAY_HOST = "localhost:10010";

    private GatewayConfigProperties gatewayConfigProperties;
    private AuthConfigurationProperties authConfigurationProperties;
    private RestTemplate restTemplate;
    private GatewaySecurityService securityService;
    private String cookie;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setup() {
        gatewayConfigProperties = GatewayConfigProperties.builder()
            .scheme(GATEWAY_SCHEME)
            .hostname(GATEWAY_HOST)
            .build();
        GatewayClient gatewayClient = new GatewayClient(gatewayConfigProperties);
        authConfigurationProperties = new AuthConfigurationProperties();
        restTemplate = mock(RestTemplate.class);
        RestResponseHandler responseHandler = new RestResponseHandler();

        securityService = new GatewaySecurityService(
            gatewayClient,
            authConfigurationProperties,
            restTemplate,
            responseHandler
        );

        cookie = String.format("%s=%s",
            authConfigurationProperties.getCookieProperties().getCookieName(), TOKEN);
    }

    @Test
    public void doSuccessfulLogin() {
        String uri = String.format("%s://%s%s", gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname(), authConfigurationProperties.getGatewayLoginEndpoint());

        HttpEntity loginRequest = createLoginRequest();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.SET_COOKIE, cookie);

        when(restTemplate.exchange(
            eq(uri),
            eq(HttpMethod.POST),
            eq(loginRequest),
            eq(String.class)))
            .thenReturn(new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT));

        Optional<String> token = securityService.login(USERNAME, PASSWORD);

        assertTrue(token.isPresent());
        assertEquals(TOKEN, token.get());
    }

    @Test
    public void doLoginWithoutCookie() {
        String uri = String.format("%s://%s%s", gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname(), authConfigurationProperties.getGatewayLoginEndpoint());

        HttpEntity loginRequest = createLoginRequest();

        when(restTemplate.exchange(
            eq(uri),
            eq(HttpMethod.POST),
            eq(loginRequest),
            eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        Optional<String> token = securityService.login(USERNAME, PASSWORD);

        assertFalse(token.isPresent());
    }

    @Test
    public void doLoginWhenGatewayUnauthorized() {
        String uri = String.format("%s://%s%s", gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname(), authConfigurationProperties.getGatewayLoginEndpoint());

        HttpEntity loginRequest = createLoginRequest();

        when(restTemplate.exchange(
            eq(uri),
            eq(HttpMethod.POST),
            eq(loginRequest),
            eq(String.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        exceptionRule.expect(BadCredentialsException.class);
        exceptionRule.expectMessage("Username or password are invalid.");

        securityService.login(USERNAME, PASSWORD);
    }

    @Test
    public void doSuccessfulQuery() {
        QueryResponse expectedQueryResponse = new QueryResponse("domain", "user", new Date(), new Date(), QueryResponse.Source.ZOWE);

        String uri = String.format("%s://%s%s", gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname(), authConfigurationProperties.getGatewayQueryEndpoint());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        HttpEntity httpEntity = new HttpEntity<>(headers);

        when(restTemplate.exchange(
            eq(uri),
            eq(HttpMethod.GET),
            eq(httpEntity),
            eq(QueryResponse.class)))
            .thenReturn(new ResponseEntity<>(expectedQueryResponse, HttpStatus.OK));

        QueryResponse query = securityService.query("token");

        assertEquals(expectedQueryResponse, query);
    }

    @Test
    public void doQueryWhenGatewayUnauthorized() {
        String uri = String.format("%s://%s%s", gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname(), authConfigurationProperties.getGatewayQueryEndpoint());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        HttpEntity httpEntity = new HttpEntity<>(headers);

        when(restTemplate.exchange(
            eq(uri),
            eq(HttpMethod.GET),
            eq(httpEntity),
            eq(QueryResponse.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        exceptionRule.expect(TokenNotValidException.class);
        exceptionRule.expectMessage("Token is not valid.");

        securityService.query("token");
    }

    private HttpEntity createLoginRequest() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode loginRequest = mapper.createObjectNode();
        loginRequest.put("username", USERNAME);
        loginRequest.put("password", PASSWORD);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return new HttpEntity<>(loginRequest, requestHeaders);
    }
}
