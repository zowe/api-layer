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

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.zowe.apiml.gateway.security.login.x509.X509AbstractMapper;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Parsed;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.security.common.error.InvalidCertificateException;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class X509AuthSourceServiceTest extends CleanCurrentRequestContextTest {
    private RequestContext context;
    private HttpServletRequest request;
    private X509Certificate x509Certificate;
    private final X509Certificate[] x509Certificates = new X509Certificate[1];

    @BeforeEach
    void init() {
        x509Certificate = mock(X509Certificate.class);
    }

    @Test
    void givenNullSource_returnNullLtpa() {
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        X509AuthSourceService service = new X509AuthSourceService(null, null, authenticationService);
        assertNull(service.getLtpaToken(null));
    }

    @Test
    void givenX509Source_returnNullLtpa() {
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        TokenCreationService tokenCreationService = mock(TokenCreationService.class);
        X509AuthSourceService service = new X509AuthSourceService(mock(X509AbstractMapper.class), tokenCreationService, authenticationService);
        assertThrows(UsernameNotFoundException.class, () -> service.getLtpaToken(new X509AuthSource(null)));
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class X509MFAuthSourceServiceTest {
        private X509AbstractMapper mapper;
        private X509AuthSourceService serviceUnderTest;

        @BeforeEach
        void init() {
            mapper = mock(X509AbstractMapper.class);
            serviceUnderTest = new X509AuthSourceService(mapper, null, null);
        }

        @Nested
        class GivenNullAuthSource {
            @Test
            void whenNoClientCertInRequest_thenAuthSourceIsNotPresent() {
                givenNoClientCertInRequest_testGetAuthSourceFromRequest(serviceUnderTest);
            }

            @Test
            void whenValidate_thenFalse() {
                Assertions.assertFalse(serviceUnderTest.isValid(null));
                verifyNoInteractions(mapper);
            }

            @Test
            void whenParse_thenNull() {
                assertNull(serviceUnderTest.parse(null));
                verifyNoInteractions(mapper);
            }
        }

        @Nested
        class GiveNullRawSource {
            @Test
            void whenValidate_thenCorrect() {
                Assertions.assertFalse(serviceUnderTest.isValid(new X509AuthSource(null)));
            }
        }

        @Nested
        class GivenValidAuthSource {
            @Test
            void whenClientCertInRequest_thenAuthSourceIsPresent() {
                givenClientCertInRequest_testGetAuthSourceFromRequest(serviceUnderTest);
            }

            @Test
            void whenValidate_thenCorrect() {
                when(mapper.isClientAuthCertificate(x509Certificate)).thenReturn(true);
                Assertions.assertTrue(serviceUnderTest.isValid(new X509AuthSource(x509Certificate)));
            }

            @Test
            void whenParse_thenCorrect() throws CertificateEncodingException {
                givenValidAuthSource_testParse(serviceUnderTest, mapper);
            }
        }

        @Nested
        class GivenIncorrectAuthSource {
            @Test
            void whenIncorrectAuthSourceType_thenIsValidFalse() {
                Assertions.assertFalse(serviceUnderTest.isValid(new JwtAuthSource("")));
            }

            @Test
            void whenAuthenticationServiceException_thenThrowWhenValidate() {
                AuthSource authSource = new X509AuthSource(x509Certificate);
                when(mapper.isClientAuthCertificate(x509Certificate)).thenThrow(new AuthenticationServiceException("Can't get extensions from certificate"));
                assertThrows(AuthenticationServiceException.class, () -> serviceUnderTest.isValid(authSource));
                verify(mapper, times(1)).isClientAuthCertificate(x509Certificate);
            }

            @Test
            void whenUnknownAuthSource_thenParsedIsNull() {
                assertNull(serviceUnderTest.parse(new JwtAuthSource("")));
                verifyNoInteractions(mapper);
            }

            @Test
            void whenAuthenticationServiceException_thenThrowWhenParse() {
                AuthSource authSource = new X509AuthSource(x509Certificate);
                when(mapper.mapCertificateToMainframeUserId(x509Certificate)).thenThrow(new AuthenticationServiceException("Can't get extensions from certificate"));
                assertThrows(AuthenticationServiceException.class, () -> serviceUnderTest.parse(authSource));
                verify(mapper, times(1)).mapCertificateToMainframeUserId(x509Certificate);
            }

            @Test
            void returnNullWhenCertificateEncodingExceptionIsThrown() throws CertificateEncodingException {
                AuthSource authSource = new X509AuthSource(x509Certificate);
                when(x509Certificate.getEncoded()).thenThrow(new CertificateEncodingException(""));
                assertNull(serviceUnderTest.parse(authSource));
            }

            @Nested
            class WhenNotAClientCertificate {
                @Test
                void thenThrowWhenValidate() {
                    AuthSource authSource = new X509AuthSource(x509Certificate);
                    when(mapper.isClientAuthCertificate(x509Certificate)).thenReturn(false);
                    assertThrows(InvalidCertificateException.class, () -> serviceUnderTest.isValid(authSource));
                    verify(mapper, times(1)).isClientAuthCertificate(x509Certificate);
                }
            }
        }
    }

    private void givenClientCertInRequest_testGetAuthSourceFromRequest(AuthSourceService serviceUnderTest) {
        x509Certificates[0] = x509Certificate;
        context = spy(RequestContext.class);
        request = mock(HttpServletRequest.class);
        RequestContext.testSetCurrentContext(context);
        when(context.getRequest()).thenReturn(request);
        when(request.getAttribute("client.auth.X509Certificate")).thenReturn(x509Certificates);

        Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest();

        verify(request, times(1)).getAttribute("client.auth.X509Certificate");

        Assertions.assertTrue(authSource.isPresent());
        Assertions.assertTrue(authSource.get() instanceof X509AuthSource);
        Assertions.assertEquals(x509Certificates[0], authSource.get().getRawSource());
    }

    private void givenNoClientCertInRequest_testGetAuthSourceFromRequest(AuthSourceService serviceUnderTest) {
        context = spy(RequestContext.class);
        request = mock(HttpServletRequest.class);
        RequestContext.testSetCurrentContext(context);
        when(context.getRequest()).thenReturn(request);
        Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest();
        verify(request, times(1)).getAttribute("client.auth.X509Certificate");
        Assertions.assertFalse(authSource.isPresent());
    }

    private void givenValidAuthSource_testParse(AuthSourceService serviceUnderTest, X509AbstractMapper mapper)
        throws CertificateEncodingException {
        String cert = "clientCertificate";
        String encodedCert = Base64.getEncoder().encodeToString(cert.getBytes());
        String distinguishedName = "distinguishedName";

        Parsed expectedParsedSource = new X509AuthSource.Parsed("user", null, null, Origin.X509, encodedCert, distinguishedName);
        when(mapper.mapCertificateToMainframeUserId(x509Certificate)).thenReturn("user");
        when(x509Certificate.getEncoded()).thenReturn(cert.getBytes());

        Principal principal = mock(Principal.class);
        when(x509Certificate.getSubjectDN()).thenReturn(principal);
        when(principal.toString()).thenReturn(distinguishedName);
        Parsed parsedSource = serviceUnderTest.parse(new X509AuthSource(x509Certificate));

        verify(mapper, times(1)).mapCertificateToMainframeUserId(x509Certificate);
        Assertions.assertNotNull(parsedSource);
        Assertions.assertEquals(expectedParsedSource, parsedSource);
    }
}
