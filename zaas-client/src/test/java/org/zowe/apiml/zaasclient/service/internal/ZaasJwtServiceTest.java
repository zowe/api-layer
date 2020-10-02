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

import org.apache.http.HttpHeaders;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.cookie.SM;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;

import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ZaasJwtServiceTest {

    private static final String JWT_TOKEN = "jwtTokenTest";
    private static final String HEADER_AUTHORIZATION = "Bearer " + JWT_TOKEN;

    private static final String COOKIE_NAME = "apimlAuthenticationToken";
    private static final String BASE_URL = "/api/v1";

    @Mock
    private CloseableHttpClient closeableHttpClient;

    @Mock
    private CloseableClientProvider closeableClientProvider;

    private ZaasJwtService zaasJwtService;

    @Before
    public void setUp() throws ZaasConfigurationException, IOException {
        doReturn(closeableHttpClient).when(closeableClientProvider).getHttpClient();
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        doReturn(new BasicStatusLine(mock(ProtocolVersion.class), 204, null))
            .when(response).getStatusLine();
        doReturn(response).when(closeableHttpClient).execute(any());
        zaasJwtService = new ZaasJwtService(closeableClientProvider, BASE_URL);
    }

    @Test
    public void givenJwtToken_whenLogout_thenSetCookie() throws ZaasClientException, IOException {
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
    public void givenAuthorizationHeaderWithJwtToken_whenLogout_thenAuthorizationHeader()
        throws ZaasClientException, IOException
    {
        zaasJwtService.logout(HEADER_AUTHORIZATION);
        verify(closeableHttpClient, times(1)).execute(
            argThat(x ->
                (x.getHeaders(HttpHeaders.AUTHORIZATION) != null) &&
                (x.getHeaders(HttpHeaders.AUTHORIZATION).length == 1) &&
                HEADER_AUTHORIZATION.equals(x.getHeaders(HttpHeaders.AUTHORIZATION)[0].getValue())
            )
        );
    }

}
