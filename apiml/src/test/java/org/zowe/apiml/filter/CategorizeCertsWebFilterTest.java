/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.security.common.verify.CertificateValidator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.security.common.filter.CategorizeCertsFilter.*;

@ExtendWith(MockitoExtension.class)
class CategorizeCertsWebFilterTest {

    @Mock
    private CertificateValidator mockCertificateValidator;
    @Mock
    private ServerWebExchange mockExchange;
    @Mock
    private WebFilterChain mockFilterChain;
    @Mock
    private ServerHttpRequest mockRequest;
    @Mock
    private ServerHttpRequest.Builder mockRequestBuilder;
    @Mock
    private ServerWebExchange.Builder mockExchangeBuilder;
    @Mock
    private SslInfo mockSslInfo;
    @Mock
    private HttpHeaders mockHeaders;

    private CategorizeCertsWebFilter filter;

    private static X509Certificate gatewayCert;
    private static X509Certificate clientCert;
    private static X509Certificate headerCert;

    @BeforeAll
    static void init() throws Exception {
        gatewayCert = loadCertificateFromKeystore("localhost", "localhost/localhost.keystore.p12");
        clientCert = loadCertificateFromKeystore("apimtst", "client_cert/client-certs.p12");
        headerCert = loadCertificateFromKeystore("user", "client_cert/client-certs.p12");

    }

    @BeforeEach
    void setUp() {

        Set<String> gatewayPublicKeys = new HashSet<>();
        gatewayPublicKeys.add(CategorizeCertsWebFilter.base64EncodePublicKey(gatewayCert));

        filter = new CategorizeCertsWebFilter(gatewayPublicKeys, mockCertificateValidator);

        when(mockExchange.getRequest()).thenReturn(mockRequest);
        when(mockExchange.mutate()).thenReturn(mockExchangeBuilder);
        when(mockExchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(mockExchangeBuilder);
        when(mockExchangeBuilder.build()).thenReturn(mockExchange);
        when(mockRequest.mutate()).thenReturn(mockRequestBuilder);
        when(mockRequestBuilder.headers(any())).thenReturn(mockRequestBuilder);
        when(mockRequestBuilder.build()).thenReturn(mockRequest);
        when(mockRequestBuilder.headers(any())).thenReturn(mockRequestBuilder);
        when(mockRequestBuilder.build()).thenReturn(mockRequest);

    }


    @Test
    void filter_whenNoTlsCerts_doesNothingAndContinuesChain() {

        when(mockRequest.getSslInfo()).thenReturn(null);
        when(mockFilterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(mockExchange, mockFilterChain);

        StepVerifier.create(result).verifyComplete();
        verify(mockExchange, never()).getAttributes();
        verify(mockFilterChain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_standardPath_withClientCertOnly() {
        Map<String, Object> attributes = new HashMap<>();
        X509Certificate[] certChain = {clientCert};

        when(mockRequest.getSslInfo()).thenReturn(mockSslInfo);
        when(mockSslInfo.getPeerCertificates()).thenReturn(certChain);
        when(mockExchange.getAttributes()).thenReturn(attributes);
        when(mockFilterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        when(mockCertificateValidator.isForwardingEnabled()).thenReturn(false);

        when(mockRequest.getHeaders()).thenReturn(mockHeaders);
        when(mockHeaders.getFirst(CLIENT_CERT_HEADER)).thenReturn("");

        StepVerifier.create(filter.filter(mockExchange, mockFilterChain)).verifyComplete();

        X509Certificate[] clientAuthCerts = (X509Certificate[]) attributes.get(ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE);
        X509Certificate[] gatewayCerts = (X509Certificate[]) attributes.get(ATTR_NAME_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE);

        assertNotNull(clientAuthCerts);
        assertEquals(1, clientAuthCerts.length);
        assertEquals(clientCert, clientAuthCerts[0]);

        assertNotNull(gatewayCerts);
        assertEquals(0, gatewayCerts.length); // No gateway certs were found
    }

    @Test
    void filter_standardPath_withGatewayCertOnly() {
        Map<String, Object> attributes = new HashMap<>();
        X509Certificate[] certChain = {gatewayCert};

        when(mockRequest.getSslInfo()).thenReturn(mockSslInfo);
        when(mockSslInfo.getPeerCertificates()).thenReturn(certChain);
        when(mockExchange.getAttributes()).thenReturn(attributes);
        when(mockFilterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        when(mockRequest.getHeaders()).thenReturn(mockHeaders);
        when(mockHeaders.getFirst(CLIENT_CERT_HEADER)).thenReturn("");
        when(mockCertificateValidator.isForwardingEnabled()).thenReturn(false);

        StepVerifier.create(filter.filter(mockExchange, mockFilterChain)).verifyComplete();

        X509Certificate[] clientAuthCerts = (X509Certificate[]) attributes.get(ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE);
        X509Certificate[] gatewayCerts = (X509Certificate[]) attributes.get(ATTR_NAME_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE);

        assertNotNull(clientAuthCerts);
        assertEquals(0, clientAuthCerts.length); // The gateway cert is not a client cert

        assertNotNull(gatewayCerts);
        assertEquals(1, gatewayCerts.length);
        assertEquals(gatewayCert, gatewayCerts[0]);
    }

    @Test
    void filter_forwardingPath_usesHeaderCertForClientAuth() throws CertificateEncodingException {
        Map<String, Object> attributes = new HashMap<>();
        X509Certificate[] tlsChain = {clientCert, gatewayCert}; // Mixed chain
        String headerCertBase64 = Base64.getEncoder().encodeToString(headerCert.getEncoded());

        when(mockRequest.getSslInfo()).thenReturn(mockSslInfo);
        when(mockSslInfo.getPeerCertificates()).thenReturn(tlsChain);
        when(mockRequest.getHeaders()).thenReturn(mockHeaders);
        when(mockHeaders.getFirst(CLIENT_CERT_HEADER)).thenReturn(headerCertBase64);
        when(mockExchange.getAttributes()).thenReturn(attributes);
        when(mockFilterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        when(mockCertificateValidator.isForwardingEnabled()).thenReturn(true);
        when(mockCertificateValidator.hasGatewayChain(tlsChain)).thenReturn(true);

        StepVerifier.create(filter.filter(mockExchange, mockFilterChain)).verifyComplete();

        X509Certificate[] clientAuthCerts = (X509Certificate[]) attributes.get(ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE);
        X509Certificate[] gatewayCerts = (X509Certificate[]) attributes.get(ATTR_NAME_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE);

        assertNotNull(clientAuthCerts);
        assertEquals(1, clientAuthCerts.length);
        assertEquals(headerCert.getSubjectX500Principal(), clientAuthCerts[0].getSubjectX500Principal());

        assertNotNull(gatewayCerts);
        assertArrayEquals(tlsChain, gatewayCerts);

        verify(mockCertificateValidator).updateAPIMLPublicKeyCertificates(tlsChain);
    }

    @Test
    void filter_alwaysRemovesClientCertHeader() {
        X509Certificate[] certChain = {clientCert};
        when(mockRequest.getSslInfo()).thenReturn(mockSslInfo);
        when(mockSslInfo.getPeerCertificates()).thenReturn(certChain);
        when(mockExchange.getAttributes()).thenReturn(new HashMap<>());
        when(mockFilterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        when(mockRequest.getHeaders()).thenReturn(mockHeaders);
        when(mockHeaders.getFirst(CLIENT_CERT_HEADER)).thenReturn("");

        StepVerifier.create(filter.filter(mockExchange, mockFilterChain)).verifyComplete();

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(mockFilterChain).filter(exchangeCaptor.capture());

        verify(mockRequestBuilder).headers(any());
    }

    private static X509Certificate loadCertificateFromKeystore(String alias, String location) throws Exception {
        var rootDir = System.getProperty("user.dir");
        var keystorePath = Paths.get(rootDir, "../keystore", location);
        try (InputStream is = Files.newInputStream(keystorePath)) {

            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(is, "password".toCharArray());
            return (X509Certificate) keystore.getCertificate(alias);
        }
    }


}
