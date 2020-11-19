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
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.cookie.SM;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.service.ZaasToken;

import javax.servlet.http.Cookie;
import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ZaasJwtServiceTest {
    private static final String JWT_TOKEN = "jwtTokenTest";
    private static final String HEADER_AUTHORIZATION = "Bearer " + JWT_TOKEN;

    private static final String COOKIE_NAME = "apimlAuthenticationToken";
    private static final String BASE_URL = "/api/v1";

    private final ObjectMapper mapper = new ObjectMapper();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private CloseableHttpClient closeableHttpClient;

    @Mock
    private CloseableClientProvider closeableClientProvider;

    private ZaasJwtService zaasJwtService;

    @Before
    public void setUp() throws ZaasConfigurationException {
        doReturn(closeableHttpClient).when(closeableClientProvider).getHttpClient();
        zaasJwtService = new ZaasJwtService(closeableClientProvider, BASE_URL);
    }

    @Test
    public void givenJwtToken_whenLogout_thenSetCookie() throws ZaasClientException, IOException {
        mockHttpClient(204);
        zaasJwtService.logout(JWT_TOKEN);
        verify(closeableHttpClient, times(1)).execute(
            argThat(x ->
                (x.getHeaders(SM.COOKIE) != null) &&
                    (x.getHeaders(SM.COOKIE).length == 1) &&
                    (COOKIE_NAME + "=" + JWT_TOKEN).equals(x.getHeaders(SM.COOKIE)[0].getValue())
            )
        );
    }

    @Test
    public void givenAuthorizationHeaderWithJwtToken_whenLogout_thenAuthorizationHeader() throws ZaasClientException, IOException {
        mockHttpClient(204);
        zaasJwtService.logout(HEADER_AUTHORIZATION);
        verify(closeableHttpClient, times(1)).execute(
            argThat(x ->
                (x.getHeaders(HttpHeaders.AUTHORIZATION) != null) &&
                    (x.getHeaders(HttpHeaders.AUTHORIZATION).length == 1) &&
                    HEADER_AUTHORIZATION.equals(x.getHeaders(HttpHeaders.AUTHORIZATION)[0].getValue())
            )
        );
    }

    @Test
    public void givenValidJwtToken_whenQueryToken_thenReturnToken() throws ZaasClientException, IOException {
        ZaasToken expectedToken = new ZaasToken();
        mockHttpClient(200, mapper.writeValueAsString(expectedToken));

        ZaasToken actualToken = zaasJwtService.query("token");
        Assert.assertEquals(expectedToken, actualToken);
    }

    @Test
    public void givenNoJwtToken_whenQueryToken_thenThrowException() throws ZaasClientException {
        setExpectedException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED, "No token provided");
        zaasJwtService.query((String) null);
    }

    @Test
    public void givenEmptyJwtToken_whenQueryToken_thenThrowException() throws ZaasClientException {
        setExpectedException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED, "No token provided");
        zaasJwtService.query("");
    }

    @Test
    public void givenInvalidJwtToken_whenQueryToken_thenThrowException() throws ZaasClientException, IOException {
        mockHttpClient(401);

        setExpectedException(ZaasClientErrorCodes.INVALID_JWT_TOKEN, "Queried token is invalid");
        zaasJwtService.query("bad token");
    }

    @Test
    public void givenExpiredToken_whenQueryToken_thenThrowException() throws ZaasClientException, IOException {
        ZaasToken expiredToken = new ZaasToken();
        expiredToken.setExpired(true);
        mockHttpClient(200, mapper.writeValueAsString(expiredToken));

        setExpectedException(ZaasClientErrorCodes.EXPIRED_JWT_EXCEPTION, "Queried token is expired");
        zaasJwtService.query("expired token");
    }

    @Test
    public void givenValidJwtTokenInRequest_whenQueryRequest_thenReturnToken() throws ZaasClientException, IOException {
        ZaasToken expectedToken = new ZaasToken();
        mockHttpClient(200, mapper.writeValueAsString(expectedToken));

        ZaasToken actualToken = zaasJwtService.query(getMockRequest("token"));
        Assert.assertEquals(expectedToken, actualToken);
    }

    @Test
    public void givenEmptyJwtTokenInRequest_whenQueryRequest_thenThrowException() throws ZaasClientException {
        setExpectedException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED, "No token provided in request");
        zaasJwtService.query(getMockRequest(""));
    }

    @Test
    public void givenNullToken_whenQueryRequest_thenThrowException() throws ZaasClientException, IOException {
        mockHttpClient(200, mapper.writeValueAsString(null));

        setExpectedException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED, "Queried token is null");
        zaasJwtService.query(getMockRequest("token"));
    }

    @Test
    public void givenExpiredToken_whenQueryRequest_thenThrowException() throws ZaasClientException, IOException {
        ZaasToken expectedToken = new ZaasToken();
        expectedToken.setExpired(true);
        mockHttpClient(200, mapper.writeValueAsString(expectedToken));

        setExpectedException(ZaasClientErrorCodes.EXPIRED_JWT_EXCEPTION, "Queried token is expired");
        zaasJwtService.query(getMockRequest("token"));
    }

    @Test
    public void givenInvalidJwtToken_whenQueryRequest_thenThrowException() throws ZaasClientException, IOException {
        mockHttpClient(401);

        setExpectedException(ZaasClientErrorCodes.INVALID_JWT_TOKEN, "Queried token is invalid");
        zaasJwtService.query("bad token");
    }

    private void mockHttpClient(int statusCode) throws IOException {
        mockHttpClient(statusCode, "");
    }

    private void mockHttpClient(int statusCode, String content) throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        doReturn(new BasicStatusLine(mock(ProtocolVersion.class), statusCode, null))
            .when(response).getStatusLine();
        HttpEntity entity = new StringEntity(content, ContentType.TEXT_PLAIN);
        doReturn(entity).when(response).getEntity();
        doReturn(response).when(closeableHttpClient).execute(any());
    }

    private MockHttpServletRequest getMockRequest(String tokenValue) {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        Cookie[] cookies = new Cookie[]{new Cookie("apimlAuthenticationToken", tokenValue)};
        mockRequest.setCookies(cookies);
        return mockRequest;
    }

    private void setExpectedException(ZaasClientErrorCodes code, String message) {
        ZaasClientException exc = new ZaasClientException(code, message);
        expectedException.expect(ZaasClientException.class);
        expectedException.expectMessage(exc.getMessage());
    }
}
