package org.zowe.apiml.gateway.ribbon;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.io.IOException;
import java.security.cert.X509Certificate;

import static org.mockito.Mockito.*;

public class ApimlCloseableHttpClientConfigTest {

    private CloseableHttpClient withCertificate;
    private CloseableHttpClient withoutCertificate;
    private CloseableHttpClient combinated;

    private SecurityContext securityContext, backupSecurityContext;

    @BeforeEach
    public void setUp() {
        withCertificate = mock(CloseableHttpClient.class);
        withoutCertificate = mock(CloseableHttpClient.class);

        combinated = new ApimlCloseableHttpClientConfig(
            withCertificate,
            withoutCertificate
        ).apimlCloseableHttpClient();

        backupSecurityContext = SecurityContextHolder.getContext();
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.setContext(backupSecurityContext);
    }

    private void setAuthentication(Authentication authentication) {
        securityContext = mock(SecurityContext.class);
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void givenNoAuthentication_whenCall_thenUseWithoutKey() throws IOException {
        setAuthentication(null);

        HttpHost arg0 = mock(HttpHost.class);
        HttpRequest arg1 = mock(HttpRequest.class);
        HttpContext arg2 = mock(HttpContext.class);

        combinated.execute(arg0, arg1, arg2);
        verify(withoutCertificate, times(1)).execute(arg0, arg1, arg2);
        verify(withCertificate, never()).execute(arg0, arg1, arg2);
    }

    @Test
    public void givenUserPassAuthentication_whenCall_thenUseWithoutKey() throws IOException {
        setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));

        HttpUriRequest arg0 = mock(HttpUriRequest.class);
        HttpContext arg1 = mock(HttpContext.class);

        combinated.execute(arg0, arg1);
        verify(withoutCertificate, times(1)).execute(arg0, arg1);
        verify(withCertificate, never()).execute(arg0, arg1);
    }

    @Test
    public void givenNonAuthenticatedAuthentication_whenCall_thenUseWithoutKey() throws IOException {
        X509Certificate x509Certificate = mock(X509Certificate.class);
        PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken = spy(
            new PreAuthenticatedAuthenticationToken("user", x509Certificate)
        );
        when(preAuthenticatedAuthenticationToken.isAuthenticated()).thenReturn(false);
        setAuthentication(preAuthenticatedAuthenticationToken);

        HttpUriRequest arg0 = mock(HttpUriRequest.class);

        combinated.execute(arg0);
        verify(withoutCertificate, times(1)).execute(arg0);
        verify(withCertificate, never()).execute(arg0);
    }

    @Test
    public void givenX509ValidAuthentication_whenCall_thenUseWithKey() throws IOException {
        X509Certificate x509Certificate = mock(X509Certificate.class);
        PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken = spy(
            new PreAuthenticatedAuthenticationToken("user", x509Certificate)
        );
        when(preAuthenticatedAuthenticationToken.isAuthenticated()).thenReturn(true);
        setAuthentication(preAuthenticatedAuthenticationToken);

        HttpHost arg0 = mock(HttpHost.class);
        HttpRequest arg1 = mock(HttpRequest.class);

        combinated.execute(arg0, arg1);
        verify(withCertificate, times(1)).execute(arg0, arg1);
        verify(withoutCertificate, never()).execute(arg0, arg1);
    }

}
