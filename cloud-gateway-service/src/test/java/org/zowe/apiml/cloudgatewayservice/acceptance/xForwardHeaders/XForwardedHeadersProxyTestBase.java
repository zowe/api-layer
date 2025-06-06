/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance.xForwardHeaders;

import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import lombok.SneakyThrows;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ResourceUtils;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.MockService;
import org.zowe.apiml.cloudgatewayservice.filters.X509awareXForwardedHeadersFilter;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Map;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.*;

class XForwardedHeadersProxyTestBase extends AcceptanceTestWithMockServices {

    RestAssuredConfig apimlCert;
    RestAssuredConfig clientCert;

    @Value("${server.ssl.keyStore}")
    private String apimlKeyStorePath;

    @Value("${server.ssl.keyStorePassword}")
    private char[] apimlKeyStorePassword;

    @Value("${server.ssl.keyPassword:}")
    private char[] apimlKeyPassword;

    @Value("${server.ssl.clientKeyStore:}")
    private String clientKeyStorePath;

    @Value("${server.ssl.clientKeyStorePassword}")
    private char[] clientKeyStorePassword;

    @Value("${server.ssl.keyPassword}")
    private char[] clientKeyPassword;

    @Value("${server.ssl.clientCN}")
    private String clientCN;

    @BeforeAll
    @SneakyThrows
    void init() {
        TrustStrategy trustStrategy = (X509Certificate[] chain, String authType) -> true;
        X509HostnameVerifier hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

        SSLContext apimlSSLContext = SSLContextBuilder.create().loadKeyMaterial(ResourceUtils.getFile(apimlKeyStorePath), apimlKeyStorePassword, apimlKeyPassword).loadTrustMaterial(null, trustStrategy).build();
        apimlCert = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(apimlSSLContext, hostnameVerifier)));

        SSLContext sslContext = SSLContextBuilder.create().loadKeyMaterial(ResourceUtils.getFile(clientKeyStorePath), clientKeyStorePassword, clientKeyPassword, (Map<String, PrivateKeyDetails> aliases, Socket socket) -> clientCN).loadTrustMaterial(null, trustStrategy).build();
        clientCert = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext, hostnameVerifier)));

    }

    @BeforeEach
    void initMockServices() throws IOException {
        mockService("serviceid1").scope(MockService.Scope.CLASS).addEndpoint("/serviceid1/xForwardedHeadersCreated").assertion(he -> assertNotNull(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.X_FORWARDED_FOR_HEADER))).responseCode(SC_OK).and().addEndpoint("/serviceid1/xForwardedHeadersForwarded").assertion(he -> assertTrue(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.X_FORWARDED_PREFIX_HEADER).contains("/test"))).assertion(he -> assertTrue(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.X_FORWARDED_FOR_HEADER).contains("1.1.1.1"))).responseCode(SC_OK).and().addEndpoint("/serviceid1/noXForwardedHeadersForwarded").assertion(he -> assertNull(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.X_FORWARDED_PREFIX_HEADER))).assertion(he -> assertFalse(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.X_FORWARDED_FOR_HEADER).contains("1.1.1.1"))).assertion(he -> assertNull(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.FORWARDED_HEADER))).responseCode(SC_OK).and().start();
    }

}
