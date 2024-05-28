/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema;

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSchemeException;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.zaas.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.X509AuthSource.Parsed;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.InvalidCertificateException;

import jakarta.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class X509SchemeTest {
    private static final String PUBLIC_KEY = "X-Certificate-Public";
    private static final String DISTINGUISHED_NAME = "X-Certificate-DistinguishedName";
    private static final String COMMON_NAME = "X-Certificate-CommonName";

    RequestContext context;
    HttpServletRequest request;
    X509Certificate x509Certificate;
    AuthSourceService authSourceService;
    AuthConfigurationProperties authConfigurationProperties;
    X509AuthSource authSource;
    X509AuthSource.Parsed parsedSource;
    X509Scheme x509Scheme;
    Authentication authentication;

    @BeforeEach
    void setup() {
        context = spy(RequestContext.class);
        RequestContext.testSetCurrentContext(context);

        request = mock(HttpServletRequest.class);
        when(context.getRequest()).thenReturn(request);

        x509Certificate = mock(X509Certificate.class);
        authSource = new X509AuthSource(x509Certificate);
        parsedSource = new Parsed("commonName", new Date(), new Date(), Origin.X509, "", "distName");

        authSourceService = mock(AuthSourceService.class);
        authConfigurationProperties = mock(AuthConfigurationProperties.class);
        doReturn(new AuthConfigurationProperties.X509Cert()).when(authConfigurationProperties).getX509Cert();
        x509Scheme = new X509Scheme(authSourceService, authConfigurationProperties);
        authentication = new Authentication(AuthenticationScheme.X509, null, null);
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenCertificateInRequest {

        @BeforeEach
        void init() {
            doReturn(true).when(authSourceService).isValid(any(AuthSource.class));
            doReturn(parsedSource).when(authSourceService).parse(any(X509AuthSource.class));
        }

        @Test
        void testGetAuthSource() {
            doReturn(Optional.empty()).when(authSourceService).getAuthSourceFromRequest(any());

            x509Scheme.getAuthSource();
            verify(authSourceService, times(1)).getAuthSourceFromRequest(any());
        }

        @Test
        void whenPublicCertificateIsRequested_onlyCorrectHeaderIsSet() {
            authentication =
                new Authentication(AuthenticationScheme.X509, null, PUBLIC_KEY);
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
            command.apply(null);
            verify(context, times(1)).addZuulRequestHeader(PUBLIC_KEY, parsedSource.getPublicKey());
            verify(context, times(0)).addZuulRequestHeader(DISTINGUISHED_NAME, parsedSource.getDistinguishedName());
            verify(context, times(0)).addZuulRequestHeader(DISTINGUISHED_NAME, parsedSource.getCommonName());
        }

        @Test
        void whenAllHeadersAreRequested_allHeadersAreSet() {
            authentication =
                new Authentication(AuthenticationScheme.X509, null, PUBLIC_KEY + "," + DISTINGUISHED_NAME + "," + COMMON_NAME);
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
            command.apply(null);

            verify(context, times(1)).addZuulRequestHeader(PUBLIC_KEY, parsedSource.getPublicKey());
            verify(context, times(1)).addZuulRequestHeader(DISTINGUISHED_NAME, parsedSource.getDistinguishedName());
            verify(context, times(1)).addZuulRequestHeader(COMMON_NAME, parsedSource.getCommonName());
        }

        @Test
        void certificatePassOnIsSetAfterApply() {
            authentication = new Authentication(AuthenticationScheme.X509, null, PUBLIC_KEY);
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
            command.apply(null);
            verify(context, atLeastOnce()).set(RoutingConstants.FORCE_CLIENT_WITH_APIML_CERT_KEY);
        }

        @Test
        void whenAuthenticationHeadersMissing_thenSendAllHeaders() {
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
            command.apply(null);
            verify(context, times(1)).addZuulRequestHeader(PUBLIC_KEY, parsedSource.getPublicKey());
            verify(context, times(1)).addZuulRequestHeader(DISTINGUISHED_NAME, parsedSource.getDistinguishedName());
            verify(context, times(1)).addZuulRequestHeader(COMMON_NAME, parsedSource.getCommonName());
        }

        @Test
        void whenUnknownAuthenticationHeader_thenNoHeaderIsSet() {
            authentication =
                new Authentication(AuthenticationScheme.X509, null, "Unknown");
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
            command.apply(null);
            verifyNoHeadersSet();
        }

        @Test
        void whenCertWithShortExpiration_thenUseCertExpiration() {
            long expectedExpiration = Instant.now().getEpochSecond() + (5 * 60 * 1000);
            parsedSource = new Parsed("commonName", new Date(), new Date(expectedExpiration), Origin.X509, "", "distName");
            doReturn(parsedSource).when(authSourceService).parse(any(AuthSource.class));
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);

            Long expiration = (Long) ReflectionTestUtils.getField(command, "expireAt");
            assertNotNull(expiration);
            assertEquals(expectedExpiration, expiration);
        }

        @Test
        void whenCertWithLongExpiration_thenUseDefaultExpiration() {
            long expectedExpiration = Instant.MAX.getEpochSecond();
            parsedSource = new Parsed("commonName", new Date(), new Date(expectedExpiration), Origin.X509, "", "distName");
            doReturn(parsedSource).when(authSourceService).parse(any(AuthSource.class));
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);

            assertNotNull(command);
            Long expiration = (Long) ReflectionTestUtils.getField(command, "expireAt");
            assertNotNull(expiration);
            assertTrue(expiration < expectedExpiration);
        }

        @Test
        void whenCannotGetExpirationFromCert_thenUseDefaultExpiration() {
            parsedSource = new Parsed("commonName", new Date(), null, Origin.X509, "", "distName");
            doReturn(parsedSource).when(authSourceService).parse(any(AuthSource.class));
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);

            assertNotNull(command);
            Long expiration = (Long) ReflectionTestUtils.getField(command, "expireAt");
            assertNotNull(expiration);
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class NoCertificateInRequest {
        @Test
        void givenNoClientCertificate_andX509SchemeRequired_thenThrows() {
            assertThrows(AuthSchemeException.class, () -> x509Scheme.createCommand(authentication, null));
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class IncorrectCertificateInRequest {
        @Test
        void givenExceptionDuringParsing_thenNoHeaderIsSet() {
            doThrow(new InvalidCertificateException("error")).when(authSourceService).parse(any(AuthSource.class));
            assertThrows(InvalidCertificateException.class, () -> x509Scheme.createCommand(authentication, authSource));
        }

        @Test
        void givenAuthSourceWithoutContent_thenThrows() {
            AuthSource nullAuthSource = new X509AuthSource(null);
            assertThrows(AuthSchemeException.class, () -> x509Scheme.createCommand(authentication, nullAuthSource));
        }

        @Test
        void givenNullParsingResult_thenNoHeaderIsSet() {
            doReturn(null).when(authSourceService).parse(any(AuthSource.class));
            assertThrows(IllegalStateException.class, () -> x509Scheme.createCommand(authentication, authSource));
        }
    }

    private void verifyNoHeadersSet() {
        verify(context, times(0)).addZuulRequestHeader(eq(PUBLIC_KEY), anyString());
        verify(context, times(0)).addZuulRequestHeader(eq(DISTINGUISHED_NAME), anyString());
        verify(context, times(0)).addZuulRequestHeader(eq(COMMON_NAME), anyString());
    }
}
