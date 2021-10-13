/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;
import org.zowe.apiml.security.client.handler.RestResponseHandler;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ErrorType;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GatewaySecurityServiceTest {
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
    private RestResponseHandler responseHandler;

    @BeforeEach
    void setup() {
        gatewayConfigProperties = GatewayConfigProperties.builder()
            .scheme(GATEWAY_SCHEME)
            .hostname(GATEWAY_HOST)
            .build();
        GatewayClient gatewayClient = new GatewayClient(gatewayConfigProperties);
        authConfigurationProperties = new AuthConfigurationProperties();
        restTemplate = mock(RestTemplate.class);
        responseHandler = spy(new RestResponseHandler());

        securityService = new GatewaySecurityService(
            gatewayClient,
            authConfigurationProperties,
            restTemplate,
            responseHandler
        );

        cookie = String.format("%s=%s",
            authConfigurationProperties.getCookieProperties().getCookieName(), TOKEN);
    }

    @Nested
    class WhenDoLogin {
        private String uri;
        private HttpEntity loginRequest;

        @BeforeEach
        void setup() {
            uri = String.format("%s://%s%s", gatewayConfigProperties.getScheme(),
                gatewayConfigProperties.getHostname(), authConfigurationProperties.getGatewayLoginEndpoint());
            loginRequest = createLoginRequest();
        }

        @Test
        void givenValidAuth_thenGetToken() {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add(HttpHeaders.SET_COOKIE, cookie);

            when(restTemplate.exchange(uri, HttpMethod.POST, loginRequest, String.class))
                .thenReturn(new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT));

            Optional<String> token = securityService.login(USERNAME, PASSWORD);

            assertTrue(token.isPresent());
            assertEquals(TOKEN, token.get());
        }

        @Test
        void givenNoCookie_thenNoToken() {
            when(restTemplate.exchange(uri, HttpMethod.POST, loginRequest, String.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

            Optional<String> token = securityService.login(USERNAME, PASSWORD);
            assertFalse(token.isPresent());
        }

        @Test
        void givenGatewayUnauthorized_thenThrowException() {
            when(restTemplate.exchange(uri, HttpMethod.POST, loginRequest, String.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

            Exception exception = assertThrows(BadCredentialsException.class, () -> securityService.login(USERNAME, PASSWORD));
            assertEquals("Username or password are invalid.", exception.getMessage());
        }

        @Nested
        class WhenHandleBadResponse {
            private static final String LOG_PARAMETER_STRING = "Cannot access Gateway service. Uri '{}' returned: {}";
            private static final String MESSAGE_KEY_STRING = "messageKey\":\"";

            @Nested
            class ThenHandleAuthGeneralError {
                @Test
                void givenNoMessageKey() {
                    String errorMessage = "my message";
                    HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.UNAUTHORIZED, errorMessage);
                    when(restTemplate.exchange(uri, HttpMethod.POST, loginRequest, String.class))
                        .thenThrow(ex);

                    assertThrows(BadCredentialsException.class, () -> securityService.login(USERNAME, PASSWORD));
                    verify(responseHandler).handleBadResponse(ex, ErrorType.AUTH_GENERAL, LOG_PARAMETER_STRING, uri, "401 " + errorMessage);
                }

                @Test
                void givenInvalidMessageKey() {
                    String errorMessage = MESSAGE_KEY_STRING + "badKey\"";
                    HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.UNAUTHORIZED, errorMessage);
                    when(restTemplate.exchange(uri, HttpMethod.POST, loginRequest, String.class))
                        .thenThrow(ex);

                    assertThrows(BadCredentialsException.class, () -> securityService.login(USERNAME, PASSWORD));
                    verify(responseHandler).handleBadResponse(ex, ErrorType.AUTH_GENERAL, LOG_PARAMETER_STRING, uri, "401 " + errorMessage);
                }
            }

            @Test
            void givenValidMessageKey_thenHandleErrorTypeForThatMessageKey() {
                String errorMessage = MESSAGE_KEY_STRING + "org.zowe.apiml.security.login.invalidCredentials\"";
                HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.UNAUTHORIZED, errorMessage);
                when(restTemplate.exchange(uri, HttpMethod.POST, loginRequest, String.class))
                    .thenThrow(ex);

                assertThrows(BadCredentialsException.class, () -> securityService.login(USERNAME, PASSWORD));
                verify(responseHandler).handleBadResponse(ex, ErrorType.BAD_CREDENTIALS, LOG_PARAMETER_STRING, uri, "401 " + errorMessage);
            }
        }

        private HttpEntity createLoginRequest() {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode loginRequest = mapper.createObjectNode();
            loginRequest.put("username", USERNAME);
            loginRequest.put("password", PASSWORD);

            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            return new HttpEntity<>(loginRequest, requestHeaders);
        }
    }

    @Nested
    class WhenDoQuery {
        private String uri;
        private HttpEntity httpEntity;

        @BeforeEach
        void setup() {
            uri = String.format("%s://%s%s", gatewayConfigProperties.getScheme(),
                gatewayConfigProperties.getHostname(), authConfigurationProperties.getGatewayQueryEndpoint());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.COOKIE, cookie);
            httpEntity = new HttpEntity<>(headers);
        }

        @Test
        void givenValidAuth_thenSuccessfulResponse() {
            QueryResponse expectedQueryResponse = new QueryResponse("domain", "user", new Date(), new Date(), QueryResponse.Source.ZOWE);
            when(restTemplate.exchange(uri, HttpMethod.GET, httpEntity, QueryResponse.class))
                .thenReturn(new ResponseEntity<>(expectedQueryResponse, HttpStatus.OK));

            QueryResponse query = securityService.query("token");
            assertEquals(expectedQueryResponse, query);
        }

        @Test
        void givenGatewayUnauthorized_thenThrowException() {
            when(restTemplate.exchange(uri, HttpMethod.GET, httpEntity, QueryResponse.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

            Exception exception = assertThrows(TokenNotValidException.class, () -> securityService.query("token"));
            assertEquals("Token is not valid.", exception.getMessage());
        }
    }
}
