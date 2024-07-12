/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.x509;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.security.common.verify.CertificateValidator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.gateway.x509.ForwardClientCertFilterFactory.CLIENT_CERT_HEADER;

class AcceptForwardedClientCertFilterFactoryTest {

    private static final String VALID_CERTIFICATE = """
        -----BEGIN CERTIFICATE-----
        MIID7zCCAtegAwIBAgIED0TPEjANBgkqhkiG9w0BAQsFADB6MQswCQYDVQQGEwJD
        WjEPMA0GA1UECBMGUHJhZ3VlMQ8wDQYDVQQHEwZQcmFndWUxFDASBgNVBAoTC1pv
        d2UgU2FtcGxlMRwwGgYDVQQLExNBUEkgTWVkaWF0aW9uIExheWVyMRUwEwYDVQQD
        Ewxab3dlIFNlcnZpY2UwHhcNMTgxMjA3MTQ1NzIyWhcNMjgxMjA0MTQ1NzIyWjB6
        MQswCQYDVQQGEwJDWjEPMA0GA1UECBMGUHJhZ3VlMQ8wDQYDVQQHEwZQcmFndWUx
        FDASBgNVBAoTC1pvd2UgU2FtcGxlMRwwGgYDVQQLExNBUEkgTWVkaWF0aW9uIExh
        eWVyMRUwEwYDVQQDEwxab3dlIFNlcnZpY2UwggEiMA0GCSqGSIb3DQEBAQUAA4IB
        DwAwggEKAoIBAQC6Orc/EJ5/t2qam1DiYU/xVbHaQrjd6uvpj2HTvOOohtFZ7/Kx
        yMAezgB8DBR4+77qXXsdP9ngnTl/i22yGwvo7Tlz6dhnQLnks7VFr1eGGC2ks+rL
        BJsF/RQexmONG9ddexWD8SOYoW9RRapQqETbcllxOenvzXruOEzaXhMazkK9Cg+J
        ucNb9HcfhIM0rjLZhqG8Gc8dAtCcxF/xHlVyFQq8fr4u2p/wGmARM14iZeQltQV7
        F3gxmw3djfcNM5S3tirPrHlZb76ZmmQEn4QiLSP198Lm+4QKAOw1dUpMf4eELO4c
        EFUHXQUCHLWc5NztZxWW40NrDbZEjcRI5ah7AgMBAAGjfTB7MB0GA1UdJQQWMBQG
        CCsGAQUFBwMCBggrBgEFBQcDATAOBgNVHQ8BAf8EBAMCBPAwKwYDVR0RBCQwIoIV
        bG9jYWxob3N0LmxvY2FsZG9tYWlugglsb2NhbGhvc3QwHQYDVR0OBBYEFHL1ygBb
        UCI/ktdk3TgQA6EJlATIMA0GCSqGSIb3DQEBCwUAA4IBAQBHALBlFf0P1TBR1MHQ
        vXYDFAW+PiyF7zP0HcrvQTAGYhF7uJtRIamapjUdIsDVbqY0RhoFnBOu8ti2z0pW
        djw47f3X/yj98n+J2aYcO64Ar+ovx93P01MA8+Mz1u/LwXk4pmrbUIcOEtyNu+vT
        a0jDobC++3Zfv5Y+iD2M8L+jacSMZNCqQByhKtTkAICXg9LMccx4XLYtJ65zGP2h
        4TEK0MMfO2G1/vUmdb3tq17zKdukj3MUS254mENCck7ioNFR0Cc9lzuSHyBrdb0x
        M/iHeamNblckK/r1roDjhCAQz9DtmETad/o7qGNFxDTRRShRV9Lww0fFB7PaV7u/
        VPx2
        -----END CERTIFICATE-----""";

    private static String encodedCert;

    static {
        encodedCert = new String(Base64.getEncoder().encode(VALID_CERTIFICATE.getBytes()));
    }

    @Test
    void givenClientCertificateInHeader_andTrustedClientCertificateInHandshake_thenUpdateWebExchange() {
        var validator = mock(CertificateValidator.class);
        when(validator.isTrusted(any())).thenReturn(true);
        var factory = new AcceptForwardedClientCertFilterFactory(validator);
        var exchange = mock(ServerWebExchange.class);
        var req = mock(ServerHttpRequest.class);
        var header = new HttpHeaders();

        header.add(CLIENT_CERT_HEADER, encodedCert);
        when(req.getHeaders()).thenReturn(header);
        when(exchange.getRequest()).thenReturn(req);
        when(req.mutate()).thenReturn(new ClientCertFilterFactoryTest.ServerHttpRequestBuilderMock());
        when(exchange.mutate()).thenReturn(new ClientCertFilterFactoryTest.ServerWebExchangeBuilderMock());
        var sslInfo = mock(SslInfo.class);
        when(req.getSslInfo()).thenReturn(sslInfo);
        var cert = new X509Certificate[]{mock(X509Certificate.class)};

        when(sslInfo.getPeerCertificates()).thenReturn(cert);
        var chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        StepVerifier.create(factory.apply(new AcceptForwardedClientCertFilterFactory.Config()).filter(exchange, chain)).expectComplete().verify();
        verify(exchange, times(1)).mutate();
    }

    @Test
    void givenClientCertificateInHeader_andInvalidClientCertificateInHandshake_thenDoNothing() {
        var validator = mock(CertificateValidator.class);
        when(validator.isTrusted(any())).thenReturn(false);
        var factory = new AcceptForwardedClientCertFilterFactory(validator);
        var exchange = mock(ServerWebExchange.class);
        var req = mock(ServerHttpRequest.class);

        when(exchange.getRequest()).thenReturn(req);
        var sslInfo = mock(SslInfo.class);
        when(req.getSslInfo()).thenReturn(sslInfo);
        var cert = new X509Certificate[]{mock(X509Certificate.class)};

        when(sslInfo.getPeerCertificates()).thenReturn(cert);
        var chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(factory.apply(new AcceptForwardedClientCertFilterFactory.Config()).filter(exchange, chain)).expectComplete().verify();
        verify(exchange, times(0)).mutate();

    }
}
