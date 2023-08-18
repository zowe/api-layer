/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.proxy;

import io.restassured.RestAssured;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.SecurityUtils;
import org.zowe.apiml.util.config.CloudGatewayConfiguration;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.TlsConfiguration;
import sun.security.provider.X509Factory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.util.requests.Endpoints.*;

@Tag("CloudGatewayProxyTest")
class CloudGatewayProxyTest {
    private static final int SECOND = 1000;
    private static final int DEFAULT_TIMEOUT = 2 * SECOND;

    static CloudGatewayConfiguration conf;
    static String trustedCerts;

    @BeforeAll
    static void init() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        conf = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();
        TlsConfiguration tlsConf = ConfigReader.environmentConfiguration().getTlsConfiguration();
        HttpsConfig config = HttpsConfig.builder()
            .keyAlias(tlsConf.getKeyAlias())
            .keyStore(tlsConf.getKeyStore())
            .keyPassword(tlsConf.getKeyPassword())
            .keyStorePassword(tlsConf.getKeyStorePassword())
            .keyStoreType(tlsConf.getKeyStoreType())
            .build();

        final Base64.Encoder mimeEncoder = Base64.getMimeEncoder(64, "\n".getBytes());
        StringBuilder sb = new StringBuilder();
        for (Certificate cert : SecurityUtils.loadCertificateChain(config)) {
            sb.append(X509Factory.BEGIN_CERT).append("\n")
            .append(mimeEncoder.encodeToString(cert.getEncoded())).append("\n")
            .append(X509Factory.END_CERT).append("\n");
        }
        trustedCerts = sb.toString();
        assertTrue(StringUtils.isNotEmpty(trustedCerts));
    }

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void givenRequestHeader_thenRouteToProvidedHost() throws URISyntaxException {
        String scgUrl = String.format("%s://%s:%s/%s", conf.getScheme(), conf.getHost(), conf.getPort(), "gateway/version");
        given().header("X-Request-Id", "gatewaygateway-service")
            .get(new URI(scgUrl)).then().statusCode(HttpStatus.SC_OK);
        given().header("X-Request-Id", "gatewaygateway-service-2")
            .get(new URI(scgUrl)).then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    void givenRequestTimeoutIsReached_thenDropConnection() {
        String scgUrl = String.format("%s://%s:%s%s?%s=%d", conf.getScheme(), conf.getHost(), conf.getPort(), DISCOVERABLE_GREET, "delayMs", DEFAULT_TIMEOUT + SECOND);
        assertTimeout(Duration.ofMillis(DEFAULT_TIMEOUT * 3), () -> {
            given()
                .header("X-Request-Id", "discoverableclientdiscoverable-client")
                .when()
                .get(scgUrl
                )
                .then()
                .statusCode(HttpStatus.SC_GATEWAY_TIMEOUT);
        });
    }

    @Test
    void givenClientCertInRequest_thenCertPassedToDomainGateway() throws URISyntaxException {
        String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), X509_ENDPOINT);
        given()
            .config(SslContext.clientCertValid)
            .header("X-Request-Id", "gatewaygateway-service")
            .when()
            .get(new URI(scgUrl))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("dn", startsWith("CN=APIMTST"))
            .body("cn", is("APIMTST"));
    }

    @Test
    void givenGatewayCertificatesRequest_thenCertificatesChainProvided() throws URISyntaxException {
        String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), CLOUD_GATEWAY_CERTIFICATES);
        String response =
            given()
                .when()
                .get(new URI(scgUrl))
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertTrue(StringUtils.isNotEmpty(response));
        assertEquals(trustedCerts, response);
    }
}
