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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.util.config.CloudGatewayConfiguration;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.TlsConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.text.ParseException;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.security.SecurityUtils.loadPublicKey;
import static org.zowe.apiml.util.requests.Endpoints.*;

@Tag("CloudGatewayProxyTest")
class CloudGatewayProxyTest {
    private static final int SECOND = 1000;
    private static final int DEFAULT_TIMEOUT = 2 * SECOND;

    static CloudGatewayConfiguration conf;
    static PublicKey publicKey;

    @BeforeAll
    static void init() {
        conf = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();
        TlsConfiguration tlsConf = ConfigReader.environmentConfiguration().getTlsConfiguration();
        HttpsConfig config = HttpsConfig.builder()
            .keyAlias(tlsConf.getKeyAlias())
            .keyStore(tlsConf.getKeyStore())
            .keyPassword(tlsConf.getKeyPassword())
            .keyStorePassword(tlsConf.getKeyStorePassword())
            .keyStoreType(tlsConf.getKeyStoreType())
            .build();
        publicKey = loadPublicKey(config);
        assertNotNull(publicKey);
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
    void givenWellKnownRequest_thenJWKSetContainsPublicKey() throws URISyntaxException, IOException, ParseException, JOSEException {
        String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), CLOUD_GATEWAY_WELL_KNOWN_JWKS);
        InputStream response =
            given()
                .when()
                .get(new URI(scgUrl))
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asInputStream();

        JWKSet jwkSet = JWKSet.load(response);
        assertNotNull(jwkSet.getKeys());
        assertFalse(jwkSet.getKeys().isEmpty());
        assertEquals(publicKey, jwkSet.getKeys().get(0).toRSAKey().toPublicKey());
    }
}
