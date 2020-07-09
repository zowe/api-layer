/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.http;

import com.netflix.zuul.context.RequestContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.cert.X509Certificate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.gateway.security.service.schema.ByPassScheme.AUTHENTICATION_SCHEME_BY_PASS_KEY;

public class HttpClientChooserTest {


    CloseableHttpClient clientWithoutCertificate = mock(CloseableHttpClient.class);
    CloseableHttpClient clientWithCertificate = mock(CloseableHttpClient.class);

    HttpClientChooser chooser = new HttpClientChooser(clientWithoutCertificate, clientWithCertificate);

    @AfterEach
    void tearDown() {
        RequestContext.getCurrentContext().clear();
    }

    @Test
    void givenBypassScheme_whenNoAuthentication_thenWithoutCertificate() {
        RequestContext.getCurrentContext().put(AUTHENTICATION_SCHEME_BY_PASS_KEY, true);
        assertThat(chooser.chooseClient(), is(clientWithoutCertificate));
    }

    @Test
    void givenNoBypassScheme_whenChoose_thenWithCertificate() {
        assertThat(chooser.chooseClient(), is(clientWithoutCertificate));
    }

    @Test
    void givenBypassScheme_whenGenericAuthentication_thenWithout() {
        RequestContext.getCurrentContext().put(AUTHENTICATION_SCHEME_BY_PASS_KEY, true);
        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));
        assertThat(chooser.chooseClient(), is(clientWithoutCertificate));
    }

    @Test
    void givenBypassSchemeAndX509Authentication_whenX509NotAuthenticated_thenWithout() {
        RequestContext.getCurrentContext().put(AUTHENTICATION_SCHEME_BY_PASS_KEY, true);
        Authentication auth = mock(Authentication.class);
        when(auth.getCredentials()).thenReturn(mock(X509Certificate.class));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(chooser.chooseClient(), is(clientWithoutCertificate));
    }
    @Test
    void givenBypassSchemeAndX509Authentication_whenX509Authenticated_thenWith() {
        RequestContext.getCurrentContext().put(AUTHENTICATION_SCHEME_BY_PASS_KEY, true);
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getCredentials()).thenReturn(mock(X509Certificate.class));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(chooser.chooseClient(), is(clientWithCertificate));
    }
    @Test
    void givenNoBypassSchemeX509Authentication_whenX509Authenticated_thenWithout() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getCredentials()).thenReturn(mock(X509Certificate.class));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(chooser.chooseClient(), is(clientWithoutCertificate));
    }
}
