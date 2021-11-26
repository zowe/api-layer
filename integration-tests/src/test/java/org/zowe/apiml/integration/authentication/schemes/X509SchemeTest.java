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
import org.junit.jupiter.api.*;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.X509Test;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.requests.Endpoints.*;

/**
 * Use Discoverable Client to verify that when the x509 certificate is used for the call to the southbound service
 * the relevant headers will be provided.
 */
@X509Test
@DiscoverableClientDependentTest
class X509SchemeTest implements TestWithStartedInstances {
    private static URI URL;

    @BeforeAll
    static void init() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
        URL = HttpRequestUtils.getUriFromGateway(X509_ENDPOINT);
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
    }
}
