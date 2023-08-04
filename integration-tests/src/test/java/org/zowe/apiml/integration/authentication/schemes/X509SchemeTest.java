/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.authentication.schemes;

import io.restassured.http.Header;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.X509Test;
import org.zowe.apiml.util.config.CloudGatewayConfiguration;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.requests.Endpoints.X509_ENDPOINT;

/**
 * Use Discoverable Client to verify that when the x509 certificate is used for the call to the southbound service
 * the relevant headers will be provided.
 */
@X509Test
@DiscoverableClientDependentTest
class X509SchemeTest implements TestWithStartedInstances {
    private static URI URL;
    static CloudGatewayConfiguration conf = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();

    @BeforeAll
    static void init() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
        URL = HttpRequestUtils.getUriFromGateway(X509_ENDPOINT);
    }

    @Test
    @Tag("CloudGatewayServiceRouting")
    void givenValidCLientCert_thenForwardDetailsInHeader() {
        String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), X509_ENDPOINT);
        given().config(SslContext.clientCertValid)
            .when()
            .get(scgUrl)
            .then()
            .body("dn", startsWith("CN=APIMTST"))
            .body("cn", is("APIMTST")).statusCode(200);
    }

    @Test
    @Tag("CloudGatewayServiceRouting")
    void givenNoCert_thenForwardErrorMessageInHeader() {
        String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), X509_ENDPOINT);
        given().config(SslContext.tlsWithoutCert)
            .when()
            .get(scgUrl)
            .then()
            .header("X-Zowe-Auth-Failure", is("ZWEAG167E No client certificate provided in the request")).statusCode(200);
    }

    @Nested
    class WhenCallingWithX509ToDiscoverableClient {
        @Nested
        class TheUsernameIsReturned {
            @Test
            void givenCorrectClientCertificateInRequest() {
                given()
                    .config(SslContext.clientCertValid)
                    .when()
                    .get(X509SchemeTest.URL)
                    .then()
                    .body("dn", startsWith("CN=APIMTST"))
                    .body("cn", is("APIMTST")).statusCode(200);
            }

            @Test
            @Tag("CloudGatewayServiceRouting")
            void givenCorrectClientCertificateInHeader() {
                String gatewayHost = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost();
                String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), X509_ENDPOINT);
                given()
                    .header("X-Request-Id", "gateway" + gatewayHost)
                    .header("x-auth-source", SslContext.clientCertValid)
                    .when()
                    .get(scgUrl)
                    .then()
                    .body("publicKey", is(not(nullValue())))
                    .body("dn", startsWith("CN=APIMTST"))
                    .body("cn", is("APIMTST")).statusCode(200);
            }

            @Test
            void givenApimlCertificateInRequest() {
                given()
                    .config(SslContext.clientCertApiml)
                    .when()
                    .get(X509SchemeTest.URL)
                    .then()
                    .body("dn", startsWith("CN="))
                    .statusCode(200);
            }
        }

        @Test
        void givenSelfSignedUntrustedCertificate_andMaliciousHeaderInRequest_thenEmptyBodyIsReturned() {
            given()
                .config(SslContext.selfSignedUntrusted)
                .header(new Header("X-Certificate-CommonName", "evil common name"))
                .header(new Header("X-Certificate-Public", "evil public key"))
                .header(new Header("X-Certificate-DistinguishedName", "evil distinguished name"))
                .when()
                .get(X509SchemeTest.URL)
                .then()
                .body("publicKey", is(""))
                .body("dn", is(""))
                .body("cn", is("")).statusCode(200);
        }

        @Test
        void givenNoCertificate_thenEmptyBodyIsReturned() {
            given()
                .when()
                .config(SslContext.tlsWithoutCert)
                .get(X509SchemeTest.URL)
                .then()
                .header("X-Zowe-Auth-Failure", is("ZWEAG167E No client certificate provided in the request")).statusCode(200);
        }
    }
}
