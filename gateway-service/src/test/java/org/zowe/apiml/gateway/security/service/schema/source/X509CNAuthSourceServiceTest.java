/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.schema.source;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.zuul.context.RequestContext;
import java.security.cert.X509Certificate;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.security.login.x509.X509CommonNameUserMapper;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.TokenCreationService;

class X509CNAuthSourceServiceTest {
    private RequestContext context;
    private HttpServletRequest request;
    private X509CommonNameUserMapper mapper;
    private X509AuthSourceService serviceUnderTest;
    private X509Certificate x509Certificate;

    @Nested
    class GivenValidAuthSource {
        @BeforeEach
        void init() {
            context = spy(RequestContext.class);
            request = mock(HttpServletRequest.class);
            RequestContext.testSetCurrentContext(context);
            x509Certificate = mock(X509Certificate.class);
            mapper = mock(X509CommonNameUserMapper.class);
            serviceUnderTest = new X509CNAuthSourceService(mapper, mock(TokenCreationService.class), mock(AuthenticationService.class));
        }

        @Test
        void whenClientCertInRequestInCustomAttribute_thenAuthSourceIsPresent() {
            when(context.getRequest()).thenReturn(request);
            when(request.getAttribute("client.auth.X509Certificate")).thenReturn(Arrays.array(x509Certificate));
            when(mapper.isClientAuthCertificate(any())).thenReturn(true);

            Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest();

            verify(request, times(1)).getAttribute("client.auth.X509Certificate");
            verify(request, times(0)).getAttribute("javax.servlet.request.X509Certificate");

            Assertions.assertTrue(authSource.isPresent());
            Assertions.assertTrue(authSource.get() instanceof X509AuthSource);
            Assertions.assertEquals(x509Certificate, authSource.get().getRawSource());
        }

        @Test
        void whenServerCertInRequest_thenThrows() {
            when(context.getRequest()).thenReturn(request);
            when(request.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(Arrays.array(x509Certificate));
            when(mapper.isClientAuthCertificate(any())).thenReturn(false);

            assertThrows(AuthSchemeException.class, () -> serviceUnderTest.getAuthSourceFromRequest());

            verify(request, times(1)).getAttribute("client.auth.X509Certificate");
            verify(request, times(1)).getAttribute("javax.servlet.request.X509Certificate");
        }

        @Test
        void whenInternalApimlCertInRequestInStandardAttribute_thenAuthSourceIsPresent() {
            when(context.getRequest()).thenReturn(request);
            when(request.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(Arrays.array(x509Certificate));
            when(mapper.isClientAuthCertificate(any())).thenReturn(true);

            Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest();

            verify(request, times(1)).getAttribute("client.auth.X509Certificate");
            verify(request, times(1)).getAttribute("javax.servlet.request.X509Certificate");

            Assertions.assertTrue(authSource.isPresent());
            Assertions.assertTrue(authSource.get() instanceof X509AuthSource);
            Assertions.assertEquals(x509Certificate, authSource.get().getRawSource());
        }
    }
}
