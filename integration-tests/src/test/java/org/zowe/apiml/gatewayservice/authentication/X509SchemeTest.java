/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice.authentication;

import io.restassured.http.Header;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.EnvironmentConfiguration;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.config.SslContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;

class X509SchemeTest {
    private final static EnvironmentConfiguration ENVIRONMENT_CONFIGURATION = ConfigReader.environmentConfiguration();
    private final static GatewayServiceConfiguration GATEWAY_SERVICE_CONFIGURATION =
        ENVIRONMENT_CONFIGURATION.getGatewayServiceConfiguration();
    private final static String SCHEME = GATEWAY_SERVICE_CONFIGURATION.getScheme();
    private final static String HOST = GATEWAY_SERVICE_CONFIGURATION.getHost();
    private final static int PORT = GATEWAY_SERVICE_CONFIGURATION.getPort();
    private final static String DISCOVERABLE_CLIENT_BASE_PATH = "/api/v1/discoverableclient";
    private static final String X509_ENDPOINT = "/x509";
    private static String URL;
    @BeforeAll
    static void init() throws Exception {
        SslContext.prepareSslAuthentication();
        URL = String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, DISCOVERABLE_CLIENT_BASE_PATH, X509_ENDPOINT);
    }

    @Test
    void givenCorrectClientCertificateInRequest_thenUsernameIsReturned() {
        given().config(SslContext.clientCertValid).get(X509SchemeTest.URL)
            .then()
            .body("dn",startsWith("CN=APIMTST"))
            .body("cn", is("APIMTST")).statusCode(200);
    }

    @Test
    void givenApimlCertificateInRequest_thenEmptyBodyIsReturned() {
        given().config(SslContext.clientCertApiml).get(X509SchemeTest.URL)
            .then()
            .body("publicKey",is(""))
            .body("dn",is(""))
            .body("cn", is("")).statusCode(200);
    }

    @Test
    void givenApimlCertificateAndMaliciousHeaderInRequest_thenEmptyBodyIsReturned() {
        given().config(SslContext.clientCertApiml)
            .header(new Header("X-Certificate-CommonName", "evil common name"))
            .header(new Header("X-Certificate-Public", "evil public key"))
            .header(new Header("X-Certificate-DistinguishedName", "evil distinguished name"))
            .get(X509SchemeTest.URL)
            .then()
            .body("publicKey",is(""))
            .body("dn",is(""))
            .body("cn", is("")).statusCode(200);

    }
}
