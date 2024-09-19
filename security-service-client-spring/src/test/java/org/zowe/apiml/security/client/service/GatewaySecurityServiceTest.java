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
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.security.client.handler.RestResponseHandler;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ErrorType;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.HttpClientMockHelper;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewaySecurityServiceTest {

    private static final String USERNAME = "user";
    private static final char[] PASSWORD = "pass".toCharArray();
    private static final char[] NEW_PASSWORD = "newPass".toCharArray();
    private static final String TOKEN = "token";
    private static final String GATEWAY_SCHEME = "https";
    private static final String GATEWAY_HOST = "localhost:10010";

    @Spy
    private RestResponseHandler responseHandler;
    @Mock
    private CloseableHttpClient closeableHttpClient;

    @Mock
    private Header header;
    @Mock
    CloseableHttpResponse response;

    private ServiceAddress gatewayConfigProperties;
    private AuthConfigurationProperties authConfigurationProperties;
    private GatewaySecurityService securityService;
    private String cookie;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MESSAGE_KEY_STRING = "messageKey\":\"";

    @BeforeEach
    void setup() {
        gatewayConfigProperties = ServiceAddress.builder()
            .scheme(GATEWAY_SCHEME)
            .hostname(GATEWAY_HOST)
            .build();
        GatewayClient gatewayClient = new GatewayClient(gatewayConfigProperties);
        authConfigurationProperties = new AuthConfigurationProperties();

        securityService = new GatewaySecurityService(
            gatewayClient,
            authConfigurationProperties,
            closeableHttpClient,
            responseHandler
        );

        cookie = String.format("%s=%s",
            authConfigurationProperties.getCookieProperties().getCookieName(), TOKEN);

        HttpClientMockHelper.mockExecuteWithResponse(closeableHttpClient, response);
    }


    @Nested
    class GivenNoContent {

        @Nested
        class WhenDoLogin {

            @BeforeEach
            void setUp() {
                when(response.getCode()).thenReturn(HttpStatus.SC_NO_CONTENT);
                when(response.getFirstHeader(HttpHeaders.SET_COOKIE)).thenReturn(header);
                when(header.getValue()).thenReturn(cookie);
            }

            @Test
            void givenValidAuth_thenGetToken() {
                Optional<String> token = securityService.login(USERNAME, PASSWORD, null);

                assertTrue(token.isPresent());
                assertEquals(TOKEN, token.get());
            }

            @Test
            void givenValidUpdatePasswordRequest_thenGetToken() {
                Optional<String> token = securityService.login(USERNAME, PASSWORD, NEW_PASSWORD);

                assertTrue(token.isPresent());
                assertEquals(TOKEN, token.get());
            }
        }

        @Nested
        class WhenDoQuery {

            @Test
            void givenValidAuth_thenSuccessfulResponse() {
                Date issued = new Date();
                Date exp = new Date(System.currentTimeMillis() + 10000);

                QueryResponse expectedQueryResponse = new QueryResponse("domain", "user", issued, exp, null, null, null);
                String responseBody = objectMapper.writeValueAsString(expectedQueryResponse);
                HttpClientMockHelper.mockResponse(response, HttpStatus.SC_NO_CONTENT, responseBody);
                QueryResponse query = securityService.query("token");
                assertEquals(expectedQueryResponse, query);
            }

            @Test
            void givenGatewayUnauthorized_thenThrowException() {
                String responseBody = MESSAGE_KEY_STRING + "org.zowe.apiml.security.query.invalidToken\"";
                HttpClientMockHelper.mockResponse(response, HttpStatus.SC_UNAUTHORIZED, responseBody);
                Exception exception = assertThrows(TokenNotValidException.class, () -> securityService.query("token"));
                assertEquals("Token is not valid.", exception.getMessage());
            }
        }

        // TODO Verify what happens on an IOException / ParseException to add coverage to this class

        @Nested
        class WhenHandleBadResponse {

            private static final String LOG_PARAMETER_STRING = "Cannot access Gateway service. Uri '{}' returned: {}";

            private String uri;

            @BeforeEach
            void setup() {
                uri = String.format("%s://%s%s", gatewayConfigProperties.getScheme(),
                    gatewayConfigProperties.getHostname(), authConfigurationProperties.getGatewayLoginEndpoint());
            }

            @Nested
            class ThenHandleAuthGeneralError {

                @Test
                void givenInvalidMessageKey() {
                    String errorMessage = MESSAGE_KEY_STRING + "badKey\"";
                    HttpClientMockHelper.mockResponse(response, HttpStatus.SC_UNAUTHORIZED, errorMessage);
                    assertThrows(BadCredentialsException.class, () -> securityService.login(USERNAME, PASSWORD, null));
                    verify(responseHandler).handleErrorType(response, ErrorType.AUTH_GENERAL, LOG_PARAMETER_STRING, uri);
                }

                @Test
                void givenGatewayUnauthorized_thenThrowException() {
                    HttpClientMockHelper.mockResponse(response, HttpStatus.SC_UNAUTHORIZED, "message");
                    Exception exception = assertThrows(BadCredentialsException.class, () -> securityService.login(USERNAME, PASSWORD, null));
                    assertEquals("Invalid Credentials", exception.getMessage());
                }

                @Test
                void givenValidMessageKey_thenHandleErrorTypeForThatMessageKey() {
                    String errorMessage = MESSAGE_KEY_STRING + "org.zowe.apiml.security.login.invalidCredentials\"";
                    HttpClientMockHelper.mockResponse(response, HttpStatus.SC_UNAUTHORIZED, errorMessage);
                    assertThrows(BadCredentialsException.class, () -> securityService.login(USERNAME, PASSWORD, null));
                    verify(responseHandler).handleErrorType(response, ErrorType.BAD_CREDENTIALS, LOG_PARAMETER_STRING, uri);
                }
            }
        }
    }
}
