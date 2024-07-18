/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema.source;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource.Parsed;

import jakarta.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultAuthSourceServiceTest {
    private X509Certificate x509Certificate;

    private JwtAuthSourceService jwtAuthSourceService;
    private X509AuthSourceService x509MFAuthSourceService;
    private PATAuthSourceService patAuthSourceService;
    private OIDCAuthSourceService oidcAuthSourceService;
    private DefaultAuthSourceService serviceUnderTest;

    private HttpServletRequest request;

    @BeforeEach
    void init() {
        jwtAuthSourceService = mock(JwtAuthSourceService.class);
        x509MFAuthSourceService = mock(X509AuthSourceService.class);
        patAuthSourceService = mock(PATAuthSourceService.class);
        oidcAuthSourceService = mock(OIDCAuthSourceService.class);
        serviceUnderTest = new DefaultAuthSourceService(jwtAuthSourceService, x509MFAuthSourceService, true, patAuthSourceService, true, oidcAuthSourceService, true);
        x509Certificate = mock(X509Certificate.class);
        request = mock(HttpServletRequest.class);
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenJwtTokenInRequest {
        private final JwtAuthSource jwtAuthSource = new JwtAuthSource("token");
        private final Parsed expectedParsedSource = new ParsedTokenAuthSource("user", new Date(111), new Date(222), Origin.ZOSMF);

        @Test
        void thenJwtAuthSourceIsPresent() {
            when(jwtAuthSourceService.getAuthSourceFromRequest(request)).thenReturn(Optional.of(jwtAuthSource));

            Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest(request);

            verify(jwtAuthSourceService, times(1)).getAuthSourceFromRequest(request);
            verifyNoInteractions(x509MFAuthSourceService);
            verifyNoInteractions(patAuthSourceService);
            verifyNoInteractions(oidcAuthSourceService);

            assertTrue(authSource.isPresent());
            assertTrue(authSource.get() instanceof JwtAuthSource);
            assertEquals(jwtAuthSource, authSource.get());
        }

        @Test
        void thenAuthSourceIsValid() {
            when(jwtAuthSourceService.isValid(any())).thenReturn(true);

            assertTrue(serviceUnderTest.isValid(jwtAuthSource));
            verify(jwtAuthSourceService, times(1)).isValid(jwtAuthSource);
            verifyNoInteractions(x509MFAuthSourceService);
            verifyNoInteractions(patAuthSourceService);
            verifyNoInteractions(oidcAuthSourceService);
        }

        @Test
        void thenAuthSourceIsParsed() {
            when(jwtAuthSourceService.parse(any())).thenReturn(expectedParsedSource);

            AuthSource.Parsed parsedAuthSource = serviceUnderTest.parse(jwtAuthSource);
            verify(jwtAuthSourceService, times(1)).parse(jwtAuthSource);
            verifyNoInteractions(x509MFAuthSourceService);
            verifyNoInteractions(patAuthSourceService);
            verifyNoInteractions(oidcAuthSourceService);
            assertEquals(expectedParsedSource, parsedAuthSource);
        }

        @Test
        void thenLtpaTokenGenerated() {
            when(jwtAuthSourceService.getLtpaToken(any())).thenReturn("ltpa");

            Assertions.assertNotNull(serviceUnderTest.getLtpaToken(jwtAuthSource));
            verify(jwtAuthSourceService, times(1)).getLtpaToken(jwtAuthSource);
            verifyNoInteractions(x509MFAuthSourceService);
            verifyNoInteractions(patAuthSourceService);
            verifyNoInteractions(oidcAuthSourceService);
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
            when(x509MFAuthSourceService.getAuthSourceFromRequest(request)).thenReturn(Optional.of(x509AuthSource));

            Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest(request);

            verify(jwtAuthSourceService, times(1)).getAuthSourceFromRequest(request);
            verify(x509MFAuthSourceService, times(1)).getAuthSourceFromRequest(request);

            assertTrue(authSource.isPresent());
            assertTrue(authSource.get() instanceof X509AuthSource);
            assertEquals(x509AuthSource, authSource.get());
        }

        @Test
        void thenAuthSourceIsValid() {
            when(x509MFAuthSourceService.isValid(any(AuthSource.class))).thenReturn(true);

            assertTrue(serviceUnderTest.isValid(x509AuthSource));
            verify(x509MFAuthSourceService, times(1)).isValid(x509AuthSource);
            verifyNoInteractions(jwtAuthSourceService);
            verifyNoInteractions(patAuthSourceService);
            verifyNoInteractions(oidcAuthSourceService);
        }

        @Test
        void thenAuthSourceIsParsed() {
            when(x509MFAuthSourceService.parse(any())).thenReturn(expectedParsedSource);

            AuthSource.Parsed parsedAuthSource = serviceUnderTest.parse(x509AuthSource);
            verify(x509MFAuthSourceService, times(1)).parse(x509AuthSource);
            verifyNoInteractions(jwtAuthSourceService);
            verifyNoInteractions(patAuthSourceService);
            verifyNoInteractions(oidcAuthSourceService);
            assertEquals(expectedParsedSource, parsedAuthSource);
        }

        @Nested
        class WhenX509IsDisabled {

            @BeforeEach
            void init() {
                serviceUnderTest = new DefaultAuthSourceService(jwtAuthSourceService, x509MFAuthSourceService, false, patAuthSourceService, true, oidcAuthSourceService, true);
            }

            @Test
            void thenAuthSourceIsEmpty() {
                Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest(request);

                verifyNoInteractions(x509MFAuthSourceService);
                assertFalse(authSource.isPresent());
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenJwtAndClientCertificateInRequest {
        private final JwtAuthSource jwtAuthSource = new JwtAuthSource("token");

        @Test
        void thenJwtAuthSourceIsPresent() {
            when(jwtAuthSourceService.getAuthSourceFromRequest(request)).thenReturn(Optional.of(jwtAuthSource));

            Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest(request);

            verify(jwtAuthSourceService, times(1)).getAuthSourceFromRequest(request);
            verifyNoInteractions(x509MFAuthSourceService);

            assertTrue(authSource.isPresent());
            assertTrue(authSource.get() instanceof JwtAuthSource);
            assertEquals(jwtAuthSource, authSource.get());
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenPersonalAccessTokenInRequest {
        private final PATAuthSource patAuthSource = new PATAuthSource("token");
        private final Parsed expectedParsedSource = new ParsedTokenAuthSource("user", new Date(111), new Date(222), Origin.ZOWE_PAT);

        @Test
        void thenJwtAuthSourceIsPresent() {
            when(patAuthSourceService.getAuthSourceFromRequest(request)).thenReturn(Optional.of(patAuthSource));

            Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest(request);

            verify(patAuthSourceService, times(1)).getAuthSourceFromRequest(request);
            verifyNoInteractions(oidcAuthSourceService);
            verifyNoInteractions(x509MFAuthSourceService);
            assertTrue(authSource.isPresent());
            assertTrue(authSource.get() instanceof PATAuthSource);
            assertEquals(patAuthSource, authSource.get());
        }

        @Test
        void thenAuthSourceIsValid() {
            when(patAuthSourceService.isValid(any())).thenReturn(true);

            assertTrue(serviceUnderTest.isValid(patAuthSource));

            verify(patAuthSourceService, times(1)).isValid(patAuthSource);
            verifyNoInteractions(jwtAuthSourceService);
            verifyNoInteractions(oidcAuthSourceService);
            verifyNoInteractions(x509MFAuthSourceService);
        }

        @Test
        void thenAuthSourceIsParsed() {
            when(patAuthSourceService.parse(any())).thenReturn(expectedParsedSource);

            AuthSource.Parsed parsedAuthSource = serviceUnderTest.parse(patAuthSource);

            verify(patAuthSourceService, times(1)).parse(patAuthSource);
            verifyNoInteractions(jwtAuthSourceService);
            verifyNoInteractions(oidcAuthSourceService);
            verifyNoInteractions(x509MFAuthSourceService);
            assertEquals(expectedParsedSource, parsedAuthSource);
        }

        @Test
        void thenLtpaTokenGenerated() {
            when(patAuthSourceService.getLtpaToken(any())).thenReturn("ltpa");

            assertNotNull(serviceUnderTest.getLtpaToken(patAuthSource));

            verify(patAuthSourceService, times(1)).getLtpaToken(patAuthSource);
            verifyNoInteractions(jwtAuthSourceService);
            verifyNoInteractions(oidcAuthSourceService);
            verifyNoInteractions(x509MFAuthSourceService);
        }

        @Nested
        class WhenPATIsDisabled {

            @BeforeEach
            void init() {
                serviceUnderTest = new DefaultAuthSourceService(jwtAuthSourceService, x509MFAuthSourceService, true, patAuthSourceService, false, oidcAuthSourceService, true);
            }

            @Test
            void thenAuthSourceIsEmpty() {
                Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest(request);

                verifyNoInteractions(patAuthSourceService);
                assertFalse(authSource.isPresent());
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenOIDCTokenInRequest {
        private final OIDCAuthSource oidcAuthSource = new OIDCAuthSource("token");
        private final Parsed expectedParsedSource = new ParsedTokenAuthSource("user", new Date(111), new Date(222), Origin.OIDC);

        @Test
        void thenJwtAuthSourceIsPresent() {
            when(oidcAuthSourceService.getAuthSourceFromRequest(request)).thenReturn(Optional.of(oidcAuthSource));

            Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest(request);

            verify(oidcAuthSourceService, times(1)).getAuthSourceFromRequest(request);
            verifyNoInteractions(x509MFAuthSourceService);
            assertTrue(authSource.isPresent());
            assertTrue(authSource.get() instanceof OIDCAuthSource);
            assertEquals(oidcAuthSource, authSource.get());
        }

        @Test
        void thenAuthSourceIsValid() {
            when(oidcAuthSourceService.isValid(any())).thenReturn(true);

            assertTrue(serviceUnderTest.isValid(oidcAuthSource));

            verify(oidcAuthSourceService, times(1)).isValid(oidcAuthSource);
            verifyNoInteractions(jwtAuthSourceService);
            verifyNoInteractions(patAuthSourceService);
            verifyNoInteractions(x509MFAuthSourceService);
        }

        @Test
        void thenAuthSourceIsParsed() {
            when(oidcAuthSourceService.parse(any())).thenReturn(expectedParsedSource);

            AuthSource.Parsed parsedAuthSource = serviceUnderTest.parse(oidcAuthSource);

            verify(oidcAuthSourceService, times(1)).parse(oidcAuthSource);
            verifyNoInteractions(jwtAuthSourceService);
            verifyNoInteractions(patAuthSourceService);
            verifyNoInteractions(x509MFAuthSourceService);
            assertEquals(expectedParsedSource, parsedAuthSource);
        }

        @Test
        void thenLtpaTokenGenerated() {
            when(oidcAuthSourceService.getLtpaToken(any())).thenReturn("ltpa");

            assertNotNull(serviceUnderTest.getLtpaToken(oidcAuthSource));

            verify(oidcAuthSourceService, times(1)).getLtpaToken(oidcAuthSource);
            verifyNoInteractions(jwtAuthSourceService);
            verifyNoInteractions(patAuthSourceService);
            verifyNoInteractions(x509MFAuthSourceService);
        }

        @Nested
        class WhenOIDCIsDisabled {

            @BeforeEach
            void init() {
                serviceUnderTest = new DefaultAuthSourceService(jwtAuthSourceService, x509MFAuthSourceService, true, patAuthSourceService, true, oidcAuthSourceService, false);
            }

            @Test
            void thenAuthSourceIsEmpty() {
                Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest(request);

                verifyNoInteractions(oidcAuthSourceService);
                assertFalse(authSource.isPresent());
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenNoAuthenticationInRequest {
        @Test
        void thenNoAuthSourceIsPresent() {
            when(jwtAuthSourceService.getAuthSourceFromRequest(request)).thenReturn(Optional.empty());
            when(x509MFAuthSourceService.getAuthSourceFromRequest(request)).thenReturn(Optional.empty());
            when(patAuthSourceService.getAuthSourceFromRequest(request)).thenReturn(Optional.empty());
            when(oidcAuthSourceService.getAuthSourceFromRequest(request)).thenReturn(Optional.empty());

            Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest(request);

            verify(jwtAuthSourceService, times(1)).getAuthSourceFromRequest(request);
            verify(x509MFAuthSourceService, times(1)).getAuthSourceFromRequest(request);
            verify(patAuthSourceService, times(1)).getAuthSourceFromRequest(request);
            verify(oidcAuthSourceService, times(1)).getAuthSourceFromRequest(request);

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
            verifyNoInteractions(patAuthSourceService);
            verifyNoInteractions(oidcAuthSourceService);
        }

        @Test
        void thenAuthSourceIsNotParsed() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> serviceUnderTest.parse(dummyAuthSource));
            verifyNoInteractions(jwtAuthSourceService);
            verifyNoInteractions(x509MFAuthSourceService);
            verifyNoInteractions(patAuthSourceService);
            verifyNoInteractions(oidcAuthSourceService);
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
