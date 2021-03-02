/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.schema;

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import static org.mockito.Mockito.*;

public class X509SchemeTest extends CleanCurrentRequestContextTest {
    private static final String PUBLIC_KEY = "X-Certificate-Public";
    private static final String DISTINGUISHED_NAME = "X-Certificate-DistinguishedName";
    private static final String COMMON_NAME = "X-Certificate-CommonName";
    RequestContext context;
    HttpServletRequest request;

    X509Certificate x509Certificate;

    @BeforeEach
    void init() throws CertificateEncodingException {
        context = spy(RequestContext.class);
        RequestContext.testSetCurrentContext(context);

        request = mock(HttpServletRequest.class);
        when(context.getRequest()).thenReturn(request);

        x509Certificate = mock(X509Certificate.class);
        X509Certificate[] x509Certificates = {x509Certificate};
        when(request.getAttribute("client.auth.X509Certificate")).thenReturn(x509Certificates);
        when(x509Certificate.getEncoded()).thenReturn(new byte[]{});
    }

    @Test
    void whenPublicCertificateIsRequested_onlyCorrectHeaderIsSet() {
        X509Scheme x509Scheme = new X509Scheme();
        Authentication authentication =
            new Authentication(AuthenticationScheme.X509, null, PUBLIC_KEY);
        X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, null);
        command.apply(null);
        verify(context, times(1)).addZuulRequestHeader(PUBLIC_KEY, "");
    }

    @Test
    void whenAllHeadersAreRequested_allHeadersAreSet() {
        X509Scheme x509Scheme = new X509Scheme();
        Authentication authentication =
            new Authentication(AuthenticationScheme.X509, null, PUBLIC_KEY + "," + DISTINGUISHED_NAME + "," + COMMON_NAME);
        Principal principal = mock(Principal.class);
        when(x509Certificate.getSubjectDN()).thenReturn(principal);
        when(principal.toString()).thenReturn("");
        X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, null);
        command.apply(null);

        verify(context, times(1)).addZuulRequestHeader(PUBLIC_KEY, "");
        verify(context, times(1)).addZuulRequestHeader(DISTINGUISHED_NAME, "");
        verify(context, times(1)).addZuulRequestHeader(COMMON_NAME, null);
    }
}
