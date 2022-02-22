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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.netflix.zuul.context.RequestContext;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.zowe.apiml.gateway.security.login.x509.X509AbstractMapper;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Parsed;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;

@ExtendWith(MockitoExtension.class)
class X509AuthSourceServiceTest extends CleanCurrentRequestContextTest {
    private RequestContext context;
    private HttpServletRequest request;
    private X509Certificate x509Certificate;

    private X509AbstractMapper mapper;

    private X509AuthSourceService serviceUnderTest;

    @BeforeEach
    void init() {
        x509Certificate = mock(X509Certificate.class);
        mapper = mock(X509AbstractMapper.class);
        serviceUnderTest = new X509AuthSourceService();
        serviceUnderTest.setMapper(mapper);
    }

    @Test
    void givenClientCertInRequest_thenAuthSourceIsPresent() {
        X509Certificate[] x509Certificates = {x509Certificate};

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

    @Test
    void givenNoClientCertInRequest_thenAuthSourceIsNotPresent() {
        context = spy(RequestContext.class);
        request = mock(HttpServletRequest.class);
        RequestContext.testSetCurrentContext(context);
        when(context.getRequest()).thenReturn(request);
        Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest();
        verify(request, times(1)).getAttribute("client.auth.X509Certificate");
        Assertions.assertFalse(authSource.isPresent());
    }

    @Test
    void givenNullAuthSource_thenAuthSourceIsInvalid() {
        Assertions.assertFalse(serviceUnderTest.isValid(null));
    }

    @Test
    void givenIncorrectAuthSourceType_thenAuthSourceIsInvalid() {
        Assertions.assertFalse(serviceUnderTest.isValid(new JwtAuthSource("")));
    }

    @Test
    void givenValidAuthSource_thenAuthSourceIsValid() {
        when(mapper.isClientAuthCertificate(x509Certificate)).thenReturn(true);
        Assertions.assertTrue(serviceUnderTest.isValid(new X509AuthSource(x509Certificate)));
    }

    @Test
    void givenIncorrectAuthSource_thenAuthSourceIsInvalid() {
        when(mapper.isClientAuthCertificate(x509Certificate)).thenReturn(false);
        Assertions.assertFalse(serviceUnderTest.isValid(new X509AuthSource(x509Certificate)));
    }

    @Test
    void givenNullAuthSource_thenParsedIsNull() {
        Assertions.assertNull(serviceUnderTest.parse(null));
        verifyNoInteractions(mapper);
    }

    @Test
    void givenUnknownAuthSource_thenParsedIsNull() {
        Assertions.assertNull(serviceUnderTest.parse(new JwtAuthSource("")));
        verifyNoInteractions(mapper);
    }

    @Test
    void givenValidAuthSource_thenParseCorrectly()
        throws CertificateEncodingException {
        String cert = "clientCertificate";
        String encodedCert = Base64.getEncoder().encodeToString(cert.getBytes());
        String distinguishedName = "distinguishedName";

        Parsed expectedParsedSource = new X509AuthSource.Parsed("user", null, null, null, encodedCert, distinguishedName);
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

    @Test
    void givenAuthenticationServiceException_thenThrowWhenParse() {
        X509AuthSource authSource = new X509AuthSource(x509Certificate);
        when(mapper.mapCertificateToMainframeUserId(x509Certificate)).thenThrow(new AuthenticationServiceException("Can't get extensions from certificate"));

        assertThrows(AuthenticationServiceException.class, () -> serviceUnderTest.parse(authSource));
        verify(mapper, times(1)).mapCertificateToMainframeUserId(x509Certificate);
    }
}
