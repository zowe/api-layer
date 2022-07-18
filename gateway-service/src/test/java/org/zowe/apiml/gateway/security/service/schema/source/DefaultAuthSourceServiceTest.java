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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Parsed;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;

@ExtendWith(MockitoExtension.class)
public class DefaultAuthSourceServiceTest extends CleanCurrentRequestContextTest {
    private X509Certificate x509Certificate;

    private JwtAuthSourceService jwtAuthSourceService;
    private X509AuthSourceService x509MFAuthSourceService;
    private PATAuthSourceService patAuthSourceService;
    private DefaultAuthSourceService serviceUnderTest;

    @BeforeEach
    void init() {
        jwtAuthSourceService = mock(JwtAuthSourceService.class);
        x509MFAuthSourceService = mock(X509AuthSourceService.class);
        patAuthSourceService = mock(PATAuthSourceService.class);
        serviceUnderTest = new DefaultAuthSourceService(jwtAuthSourceService, x509MFAuthSourceService, patAuthSourceService, true);
        x509Certificate = mock(X509Certificate.class);
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenJwtTokenInRequest {
        private final JwtAuthSource jwtAuthSource = new JwtAuthSource("token");
        private final Parsed expectedParsedSource = new JwtAuthSource.Parsed("user", new Date(111), new Date(222), Origin.ZOSMF);

        @Test
        void thenJwtAuthSourceIsPresent() {
            when(jwtAuthSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(jwtAuthSource));

            Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest();

            verify(jwtAuthSourceService, times(1)).getAuthSourceFromRequest();
            verifyNoInteractions(x509MFAuthSourceService);

            assertTrue(authSource.isPresent());
            assertTrue(authSource.get() instanceof JwtAuthSource);
            Assertions.assertEquals(jwtAuthSource, authSource.get());
        }

        @Test
        void thenAuthSourceIsValid() {
            when(jwtAuthSourceService.isValid(any())).thenReturn(true);

            assertTrue(serviceUnderTest.isValid(jwtAuthSource));
            verify(jwtAuthSourceService, times(1)).isValid(jwtAuthSource);
            verifyNoInteractions(x509MFAuthSourceService);
        }

        @Test
        void thenAuthSourceIsParsed() {
            when(jwtAuthSourceService.parse(any())).thenReturn(expectedParsedSource);

            AuthSource.Parsed parsedAuthSource = serviceUnderTest.parse(jwtAuthSource);
            verify(jwtAuthSourceService, times(1)).parse(jwtAuthSource);
            verifyNoInteractions(x509MFAuthSourceService);
            Assertions.assertEquals(expectedParsedSource, parsedAuthSource);
        }

        @Test
        void thenLtpaTokenGenerated() {
            when(jwtAuthSourceService.getLtpaToken(any())).thenReturn("ltpa");

            Assertions.assertNotNull(serviceUnderTest.getLtpaToken(jwtAuthSource));
            verify(jwtAuthSourceService, times(1)).getLtpaToken(jwtAuthSource);
            verifyNoInteractions(x509MFAuthSourceService);
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenClientCertificateInRequest {
        private X509AuthSource x509AuthSource;
        private final Parsed expectedParsedSource = new X509AuthSource.Parsed("user", new Date(111), new Date(222), Origin.ZOSMF, "encoded", "distName");

        @BeforeEach
        void init() {
            x509Certificate = mock(X509Certificate.class);
            x509AuthSource = new X509AuthSource(x509Certificate);
        }

        @Test
        void thenX509AuthSourceIsPresent() {
            when(x509MFAuthSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(x509AuthSource));

            Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest();

            verify(jwtAuthSourceService, times(1)).getAuthSourceFromRequest();
            verify(x509MFAuthSourceService, times(1)).getAuthSourceFromRequest();

            assertTrue(authSource.isPresent());
            assertTrue(authSource.get() instanceof X509AuthSource);
            Assertions.assertEquals(x509AuthSource, authSource.get());
        }

        @Test
        void thenAuthSourceIsValid() {
            when(x509MFAuthSourceService.isValid(any(AuthSource.class))).thenReturn(true);

            assertTrue(serviceUnderTest.isValid(x509AuthSource));
            verify(x509MFAuthSourceService, times(1)).isValid(x509AuthSource);
            verifyNoInteractions(jwtAuthSourceService);
        }

        @Test
        void thenAuthSourceIsParsed() {
            when(x509MFAuthSourceService.parse(any())).thenReturn(expectedParsedSource);

            AuthSource.Parsed parsedAuthSource = serviceUnderTest.parse(x509AuthSource);
            verify(x509MFAuthSourceService, times(1)).parse(x509AuthSource);
            verifyNoInteractions(jwtAuthSourceService);
            Assertions.assertEquals(expectedParsedSource, parsedAuthSource);
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenJwtAndClientCertificateInRequest {
        private final JwtAuthSource jwtAuthSource = new JwtAuthSource("token");

        @Test
        void thenJwtAuthSourceIsPresent() {
            when(jwtAuthSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(jwtAuthSource));

            Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest();

            verify(jwtAuthSourceService, times(1)).getAuthSourceFromRequest();
            verifyNoInteractions(x509MFAuthSourceService);

            assertTrue(authSource.isPresent());
            assertTrue(authSource.get() instanceof JwtAuthSource);
            Assertions.assertEquals(jwtAuthSource, authSource.get());
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenNoAuthenticationInRequest {
        @Test
        void thenX509AuthSourceIsPresent() {
            when(jwtAuthSourceService.getAuthSourceFromRequest()).thenReturn(Optional.empty());
            when(x509MFAuthSourceService.getAuthSourceFromRequest()).thenReturn(Optional.empty());
            when(patAuthSourceService.getAuthSourceFromRequest()).thenReturn(Optional.empty());

            Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest();

            verify(jwtAuthSourceService, times(1)).getAuthSourceFromRequest();
            verify(x509MFAuthSourceService, times(1)).getAuthSourceFromRequest();

            assertFalse(authSource.isPresent());
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenUnknownAuthSource {
        private final DummyAuthSource dummyAuthSource = new DummyAuthSource();

        @Test
        void thenAuthSourceIsInvalid() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> serviceUnderTest.isValid(dummyAuthSource));
            verifyNoInteractions(jwtAuthSourceService);
            verifyNoInteractions(x509MFAuthSourceService);
        }

        @Test
        void thenAuthSourceIsNotParsed() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> serviceUnderTest.parse(dummyAuthSource));
            verifyNoInteractions(jwtAuthSourceService);
            verifyNoInteractions(x509MFAuthSourceService);
        }
    }

    static class DummyAuthSource implements AuthSource {
        @Override
        public Object getRawSource() {
            return null;
        }

        @Override
        public AuthSourceType getType() {
            return null;
        }
    }
}
