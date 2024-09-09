/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaasclient.service.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.util.HttpClientMockHelper;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.service.ZaasToken;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ZaasJwtServiceTest {
    private static final String JWT_TOKEN = "jwtTokenTest";
    private static final String HEADER_AUTHORIZATION = "Bearer " + JWT_TOKEN;

    private static final String COOKIE_NAME = "apimlAuthenticationToken";
    private static final String BASE_URL = "/api/v1";

    private final ObjectMapper mapper = new ObjectMapper();

    private final ConfigProperties configProperties = new ConfigProperties();
    @Captor
    ArgumentCaptor<HttpUriRequestBase> requestCaptor;

    private static final String EXPIRED_PASSWORD_RESPONSE =
        //language=JSON
        """
            {
                "messages": [
                    {
                        "messageType": "ERROR",
                        "messageNumber": "ZWEAT412E",
                        "messageContent": "The password for the specified identity has expired",
                        "messageKey": "org.zowe.apiml.security.platform.errno.EMVSEXPIRE"
                    }
                ]
            }""";

    @Mock
    private CloseableHttpClient closeableHttpClient;

    private ZaasJwtService zaasJwtService;

    @BeforeEach
    void setUp() {
        zaasJwtService = new ZaasJwtService(closeableHttpClient, BASE_URL, configProperties);
    }

    @Test
    void givenJwtToken_whenLogout_thenSetCookie() throws ZaasClientException, IOException {
        mockHttpClientResponse(204);
        zaasJwtService.logout(JWT_TOKEN);
        verify(closeableHttpClient, times(1)).execute(requestCaptor.capture(), any(HttpClientResponseHandler.class));
        var capturedRequest = requestCaptor.getValue();
        assertTrue((capturedRequest.getHeaders(HttpHeaders.COOKIE) != null) &&
            (capturedRequest.getHeaders(HttpHeaders.COOKIE).length == 1) &&
            (COOKIE_NAME + "=" + JWT_TOKEN).equals(capturedRequest.getHeaders(HttpHeaders.COOKIE)[0].getValue()));
    }

    @Test
    void givenAuthorizationHeaderWithJwtToken_whenLogout_thenAuthorizationHeader() throws ZaasClientException, IOException {
        mockHttpClientResponse(204);
        zaasJwtService.logout(HEADER_AUTHORIZATION);

        verify(closeableHttpClient, times(1)).execute(requestCaptor.capture(), any(HttpClientResponseHandler.class));
        var capturedRequest = requestCaptor.getValue();
        assertTrue((capturedRequest.getHeaders(HttpHeaders.AUTHORIZATION) != null) &&
            (capturedRequest.getHeaders(HttpHeaders.AUTHORIZATION).length == 1) &&
            HEADER_AUTHORIZATION.equals(capturedRequest.getHeaders(HttpHeaders.AUTHORIZATION)[0].getValue()));
    }

    @Test
    void givenValidJwtToken_whenQueryToken_thenReturnToken() throws ZaasClientException, IOException {
        ZaasToken expectedToken = new ZaasToken();
        mockHttpClientResponse(200, mapper.writeValueAsString(expectedToken));

        ZaasToken actualToken = zaasJwtService.query("token");
        assertEquals(expectedToken, actualToken);
    }

    @Test
    void givenNoJwtToken_whenQueryToken_thenThrowException() {
        zaasClientTestAssertThrows(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED, "No token provided", () -> zaasJwtService.query((String) null));
    }

    @Test
    void givenEmptyJwtToken_whenQueryToken_thenThrowException() {
        zaasClientTestAssertThrows(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED, "No token provided", () -> zaasJwtService.query(""));
    }

    @Test
    void givenInvalidJwtToken_whenQueryToken_thenThrowException() {
        mockHttpClientResponse(401);
        zaasClientTestAssertThrows(ZaasClientErrorCodes.INVALID_JWT_TOKEN, "Queried token is invalid or expired", () -> zaasJwtService.query("bad token"));
    }

    @Test
    void givenExpiredToken_whenQueryToken_thenThrowException() throws IOException {
        ZaasToken expiredToken = new ZaasToken();
        expiredToken.setExpired(true);

        mockHttpClientResponse(200, mapper.writeValueAsString(expiredToken));
        zaasClientTestAssertThrows(ZaasClientErrorCodes.EXPIRED_JWT_EXCEPTION, "Queried token is expired", () -> zaasJwtService.query("expired token"));
    }

    @Test
    void givenJwtTokenInCookie_whenQueryRequest_thenReturnToken() throws ZaasClientException, IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        Cookie[] cookies = new Cookie[]{new Cookie(COOKIE_NAME, "token")};
        mockRequest.setCookies(cookies);

        ZaasToken expectedToken = new ZaasToken();
        mockHttpClientResponse(200, mapper.writeValueAsString(expectedToken));

        ZaasToken actualToken = zaasJwtService.query(mockRequest);
        assertEquals(expectedToken, actualToken);
    }

    @Test
    void givenJwtTokenInHeader_whenQueryRequest_thenReturnToken() throws ZaasClientException, IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(HttpHeaders.AUTHORIZATION, HEADER_AUTHORIZATION);

        ZaasToken expectedToken = new ZaasToken();
        mockHttpClientResponse(200, mapper.writeValueAsString(expectedToken));

        ZaasToken actualToken = zaasJwtService.query(mockRequest);
        assertEquals(expectedToken, actualToken);
    }

    @Test
    void givenNoJwtTokenInRequest_whenQueryRequest_thenThrowException() {
        zaasClientTestAssertThrows(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED, "No token provided", () -> zaasJwtService.query(new MockHttpServletRequest()));
    }

    @Test
    void givenHeadersAndCookiesButNoTokenInRequest_whenQueryRequest_thenThrowException() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        Cookie[] cookies = new Cookie[]{new Cookie("cookie", "value")};
        mockRequest.setCookies(cookies);
        mockRequest.addHeader("header", "value");

        zaasClientTestAssertThrows(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED, "No token provided", () -> zaasJwtService.query(mockRequest));
    }

    @Test
    void givenHeaderWithEmptyAuthorization_whenQueryRequest_thenThrowException() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "");

        zaasClientTestAssertThrows(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED, "No token provided", () -> zaasJwtService.query(mockRequest));
    }

    @Test
    void givenExpiredPassword_whenLogin_thenThrowException() {
        var responseMock = mockHttpClientResponse(401, EXPIRED_PASSWORD_RESPONSE);
        when(responseMock.getHeaders(HttpHeaders.SET_COOKIE)).thenReturn(new Header[0]);
        zaasClientTestAssertThrows(ZaasClientErrorCodes.EXPIRED_PASSWORD, "The specified password is expired",
            () -> zaasJwtService.login("user", "password".toCharArray()));
    }

    @Test
    void givenExpiredPassword_whenQuery_thenThrowException() {
        mockHttpClientResponse(401, EXPIRED_PASSWORD_RESPONSE);
        zaasClientTestAssertThrows(ZaasClientErrorCodes.EXPIRED_PASSWORD, "The specified password is expired",
            () -> zaasJwtService.query("jwt"));
    }

    private void mockHttpClientResponse(int statusCode) {
        mockHttpClientResponse(statusCode, "null");
    }

    private CloseableHttpResponse mockHttpClientResponse(int statusCode, String content) {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        doReturn(statusCode).when(response).getCode();
        HttpEntity entity = new StringEntity(content, ContentType.TEXT_PLAIN);
        doReturn(entity).when(response).getEntity();
        HttpClientMockHelper.mockExecuteWithResponse(closeableHttpClient, response);
        return response;
    }

    private void zaasClientTestAssertThrows(ZaasClientErrorCodes code, String message, Executable executable) {
        ZaasClientException exc = new ZaasClientException(code, message);
        Exception exception = assertThrows(ZaasClientException.class, executable);
        assertEquals(exc.getMessage(), exception.getMessage());
    }
}
