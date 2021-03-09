/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cachingservice;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.service.DiscoveryUtils;

import java.util.List;

import static io.restassured.RestAssured.given;


class WebSecurityTest implements TestWithStartedInstances {

    private static final String CACHING_PATH = "/cachingservice/api/v1/cache";
    private static final String HEALTH_PATH = "/cachingservice/application/health";
    private static final String INFO_PATH = "/cachingservice/application/info";
    private static final String APIDOC_PATH = "/cachingservice/v2/api-docs";

    private String caching_url;
    private static final String CERT_HEADER_NAME = "X-Certificate-DistinguishedName";

    @BeforeAll
    static void setup() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication();
    }

    @BeforeEach
    void setupCachingUrl() {
        List<DiscoveryUtils.InstanceInfo> cachingInstances = DiscoveryUtils.getInstances("cachingservice");
        caching_url = cachingInstances.stream().findFirst().map(i -> String.format("%s", i.getUrl()))
            .orElseThrow(() -> new RuntimeException("Cannot determine Caching service from Discovery"));
    }

    @Nested
    class calledWithoutTokenOrCertificate {

        @BeforeEach
        void setUp() {
            clearSsl();
        }

        @Test
        void healthInfoApidocAreAccessible() {
            given()
                .when().get(caching_url + HEALTH_PATH).then().statusCode(HttpStatus.OK.value());
            given()
                .when().get(caching_url + INFO_PATH).then().statusCode(HttpStatus.OK.value());
            given()
                .when().get(caching_url + APIDOC_PATH).then().statusCode(HttpStatus.OK.value());
        }
    }

    @Nested
    class calledWithHeaderAndCertificate {

        @BeforeEach
        void setUp() {
            clearSsl();
        }

        @Test
        void cachingApiEndpointsAccessible() {

            given().config(SslContext.clientCertApiml)
                .header(CERT_HEADER_NAME, "value")
                .when().get(caching_url + CACHING_PATH)
                .then().statusCode(HttpStatus.OK.value());

            given().config(SslContext.selfSignedUntrusted)
                .header(CERT_HEADER_NAME, "value")
                .when().get(caching_url + CACHING_PATH)
                .then().statusCode(HttpStatus.FORBIDDEN.value());

            given()
                .header(CERT_HEADER_NAME, "value")
                .when().get(caching_url + CACHING_PATH)
                .then().statusCode(HttpStatus.FORBIDDEN.value());

            given().config(SslContext.clientCertApiml)
                .when().get(caching_url + CACHING_PATH)
                .then().statusCode(HttpStatus.UNAUTHORIZED.value());

            given()
                .when().get(caching_url + CACHING_PATH)
                .then().statusCode(HttpStatus.FORBIDDEN.value());
        }
    }

    private void clearSsl() {
        RestAssured.config = RestAssured.config().sslConfig(SSLConfig.sslConfig());
        RestAssured.useRelaxedHTTPSValidation();
    }
}
