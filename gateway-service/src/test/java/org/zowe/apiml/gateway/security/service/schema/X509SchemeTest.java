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
import java.security.Principal;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource.Parsed;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class X509SchemeTest extends CleanCurrentRequestContextTest {
    private static final String PUBLIC_KEY = "X-Certificate-Public";
    private static final String DISTINGUISHED_NAME = "X-Certificate-DistinguishedName";
    private static final String COMMON_NAME = "X-Certificate-CommonName";
    RequestContext context;
    HttpServletRequest request;

    X509Certificate x509Certificate;

    AuthSourceService authSourceService;
    X509AuthSource authSource;
    X509AuthSource.Parsed parsedSource;
    X509Scheme x509Scheme;

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenCertificateInRequest {

        @BeforeEach
        void init() throws CertificateEncodingException {
            context = spy(RequestContext.class);
            RequestContext.testSetCurrentContext(context);

            request = mock(HttpServletRequest.class);
            when(context.getRequest()).thenReturn(request);

            x509Certificate = mock(X509Certificate.class);

            when(x509Certificate.getEncoded()).thenReturn(new byte[]{});
            authSourceService = mock(AuthSourceService.class);

            authSource = new X509AuthSource(x509Certificate);
            parsedSource = new Parsed("commonName", new Date(), new Date(), Origin.X509, "", "distName");

            doReturn(true).when(authSourceService).isValid(any(AuthSource.class));
            doReturn(parsedSource).when(authSourceService).parse(any(AuthSource.class));
            doReturn(Optional.of(new X509AuthSource(x509Certificate))).when(authSourceService).getAuthSourceFromRequest();

            x509Scheme = new X509Scheme(authSourceService);
        }

        @Test
        void whenPublicCertificateIsRequested_onlyCorrectHeaderIsSet() {
            Authentication authentication =
                new Authentication(AuthenticationScheme.X509, null, PUBLIC_KEY);
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
            command.apply(null);
            verify(context, times(1)).addZuulRequestHeader(PUBLIC_KEY, parsedSource.getPublicKey());
            verify(context, times(0)).addZuulRequestHeader(DISTINGUISHED_NAME, parsedSource.getDistinguishedName());
            verify(context, times(0)).addZuulRequestHeader(DISTINGUISHED_NAME, parsedSource.getCommonName());
        }

        @Test
        void whenAllHeadersAreRequested_allHeadersAreSet() {
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

        @Test
        void certificatePassOnIsSetAfterApply() {
            Authentication authentication =
                new Authentication(AuthenticationScheme.X509, null, PUBLIC_KEY);
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
            command.apply(null);
            verify(context, atLeastOnce()).set(RoutingConstants.FORCE_CLIENT_WITH_APIML_CERT_KEY);
        }

        @Test
        void whenAuthenticationHeadersMissing_thenSendAllHeaders() {
            Authentication authentication =
                new Authentication(AuthenticationScheme.X509, null, null);
            Principal principal = mock(Principal.class);
            when(x509Certificate.getSubjectDN()).thenReturn(principal);
            when(principal.toString()).thenReturn("");
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, null);
            command.apply(null);
            verify(context, times(1)).addZuulRequestHeader(PUBLIC_KEY, "");
            verify(context, times(1)).addZuulRequestHeader(DISTINGUISHED_NAME, "");
            verify(context, times(1)).addZuulRequestHeader(COMMON_NAME, null);
        }

        @Test
        void whenUnknownAuthenticationHeader_thenNoHeaderIsSet() {
            Authentication authentication =
                new Authentication(AuthenticationScheme.X509, null, "Unknown");
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
            command.apply(null);
            verifyNoHeadersSet();
        }
    }

    @Nested
    class NoCertificateInRequest {
        @BeforeEach
        void init() {
            context = spy(RequestContext.class);
            RequestContext.testSetCurrentContext(context);

            request = mock(HttpServletRequest.class);
            when(context.getRequest()).thenReturn(request);
            authSourceService = mock(AuthSourceService.class);

            x509Scheme = new X509Scheme(authSourceService);
        }

        @Test
        void givenNoClientCertificate_andX509SchemeRequired_thenNoHeaderIsSet() {
            doReturn(Optional.empty()).when(authSourceService).getAuthSourceFromRequest();

            Authentication authentication =
                new Authentication(AuthenticationScheme.X509, null, null);
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, null);
            command.apply(null);
            verifyNoHeadersSet();
        }
    }

    private void verifyNoHeadersSet() {
        verify(context, times(0)).addZuulRequestHeader(eq(PUBLIC_KEY), anyString());
        verify(context, times(0)).addZuulRequestHeader(eq(DISTINGUISHED_NAME), anyString());
        verify(context, times(0)).addZuulRequestHeader(eq(COMMON_NAME), anyString());
    }

}
