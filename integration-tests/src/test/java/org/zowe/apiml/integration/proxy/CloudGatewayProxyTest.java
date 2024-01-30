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
import org.junit.jupiter.api.*;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.SecurityUtils;
import org.zowe.apiml.util.config.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.util.requests.Endpoints.*;

@Tag("CloudGatewayProxyTest")
class CloudGatewayProxyTest {
    private static final int SECOND = 1000;
    private static final int DEFAULT_TIMEOUT = 2 * SECOND;

    private static final String HEADER_X_FORWARD_TO = "X-Forward-To";

    static CloudGatewayConfiguration conf;

    @BeforeAll
    static void init() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());

        conf = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();
    }

    @Disabled
    void givenRequestHeader_thenRouteToProvidedHost() throws URISyntaxException {
        String scgUrl = String.format("%s://%s:%s/%s", conf.getScheme(), conf.getHost(), conf.getPort(), "gateway/version");
        given().header(HEADER_X_FORWARD_TO, "apiml1")
            .get(new URI(scgUrl)).then().statusCode(200);
        given().header(HEADER_X_FORWARD_TO, "apiml2")
            .get(new URI(scgUrl)).then().statusCode(200);
    }

    @Disabled
    void givenBasePath_thenRouteToProvidedHost() throws URISyntaxException {
        String scgUrl1 = String.format("%s://%s:%s/%s", conf.getScheme(), conf.getHost(), conf.getPort(), "apiml1/gateway/version");
        String scgUrl2 = String.format("%s://%s:%s/%s", conf.getScheme(), conf.getHost(), conf.getPort(), "apiml2/gateway/version");
        given().get(new URI(scgUrl1)).then().statusCode(200);
        given().get(new URI(scgUrl2)).then().statusCode(200);
    }

    @Test
    void givenRequestTimeoutIsReached_thenDropConnection() {
        String scgUrl = String.format("%s://%s:%s%s?%s=%d", conf.getScheme(), conf.getHost(), conf.getPort(), DISCOVERABLE_GREET, "delayMs", DEFAULT_TIMEOUT + SECOND);
        assertTimeout(Duration.ofMillis(DEFAULT_TIMEOUT * 3), () -> {
            given()
                .header(HEADER_X_FORWARD_TO, "discoverableclient")
            .when()
                .get(scgUrl)
            .then()
                .statusCode(HttpStatus.SC_GATEWAY_TIMEOUT);
        });
    }

    @Nested
    class GivenClientCertificateInRequest {

        @Disabled
        void givenRequestHeader_thenCertPassedToDomainGateway() {
            String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), X509_ENDPOINT);
            given()
                .config(SslContext.clientCertValid)
                .header(HEADER_X_FORWARD_TO, "apiml1")
                .when()
                .get(scgUrl)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("dn", startsWith("CN=APIMTST"))
                .body("cn", is("APIMTST"));
        }

        @Disabled
        void givenBasePath_thenCertPassedToDomainGateway() {
            String scgUrl = String.format("%s://%s:%s/%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), "apiml1", X509_ENDPOINT);
            given()
                .config(SslContext.clientCertValid)
                .when()
                .get(scgUrl)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("dn", startsWith("CN=APIMTST"))
                .body("cn", is("APIMTST"));
        }
    }

    @Nested
    class GivenGatewayCertificatesRequest {

        private final String trustedCerts;

        {
            TlsConfiguration tlsConf = ConfigReader.environmentConfiguration().getTlsConfiguration();
            HttpsConfig httpsConf = HttpsConfig.builder()
                .keyAlias(tlsConf.getKeyAlias())
                .keyStore(tlsConf.getKeyStore())
                .keyPassword(tlsConf.getKeyPassword())
                .keyStorePassword(tlsConf.getKeyStorePassword())
                .keyStoreType(tlsConf.getKeyStoreType())
                .build();

            // build the expected certificate chain
            final Base64.Encoder mimeEncoder = Base64.getMimeEncoder(64, "\n".getBytes());
            StringBuilder sb = new StringBuilder();
            try {
                for (Certificate cert : SecurityUtils.loadCertificateChain(httpsConf)) {
                    sb.append("-----BEGIN CERTIFICATE-----\n")
                        .append(mimeEncoder.encodeToString(cert.getEncoded())).append("\n")
                        .append("-----END CERTIFICATE-----\n");
                }
            } catch (Exception e) {
                fail("Failed to load certificate chain.", e);
            }
            trustedCerts = sb.toString();
            assertTrue(StringUtils.isNotEmpty(trustedCerts));
        }

        @Test
        void thenCertificatesChainProvided() throws URISyntaxException {
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
}
