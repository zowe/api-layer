/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.eureka;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.zowe.apiml.discovery.functional.DiscoveryFunctionalTest;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;

@ActiveProfiles("https")
@TestInstance(Lifecycle.PER_CLASS)
public class EurekaEndpointTest extends DiscoveryFunctionalTest {

    @Value("${server.ssl.keyPassword}")
    char[] password;
    @Value("${server.ssl.keyStore}")
    String client_cert_keystore;
    @Value("${server.ssl.keyStore}")
    String keystore;

    @BeforeEach
    void setup() throws Exception {
        SslContextConfigurer configurer = new SslContextConfigurer(password, client_cert_keystore, keystore);
        SslContext.prepareSslAuthentication(configurer);
    }

    @BeforeAll
    void init() {
        SslContext.reset();
    }

    @AfterAll
    void tearDown() {
        SslContext.reset();
    }

    @Override
    protected String getProtocol() {
        return "https";
    }

    @Test
    void givenInvalidService_whenRenewInstance_thenReturnEmptyBody() {
        given()
            .config(SslContext.clientCertApiml)
            .contentType(ContentType.JSON)
            .log().all()
        .when()
            .put(getDiscoveryUriWithPath("/eureka/apps/unknown-service-id/unknown-instance-id"))
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body(equalTo(""));
    }

}
