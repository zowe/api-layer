/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.functional.caching;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.CachingServiceTest;
import org.zowe.apiml.util.categories.NotAttlsTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.service.DiscoveryUtils;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

/**
 * This test is specifically testing the access to the Caching service. It doesn't go through the Gateway.
 */
@CachingServiceTest
class CachingAuthenticationTest implements TestWithStartedInstances {

    private static final String CACHING_PATH = "/cachingservice/api/v1/cache";
    private static final String HEALTH_PATH = "/cachingservice/application/health";
    private static final String INFO_PATH = "/cachingservice/application/info";
    private static final String APIDOC_PATH = "/cachingservice/v3/api-docs";

    private String caching_url = ConfigReader.environmentConfiguration().getCachingServiceConfiguration().getUrl();
    private static final String CERT_HEADER_NAME = "X-Certificate-DistinguishedName";

    @BeforeAll
    static void setup() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
    }

    @BeforeEach
    void setupCachingUrl() {
        // TODO: Move to the DiscoveryRequests
        List<DiscoveryUtils.InstanceInfo> cachingInstances = DiscoveryUtils.getInstances("cachingservice");
        if (StringUtils.isEmpty(caching_url)) {
            caching_url = cachingInstances.stream().findFirst().map(i -> String.format("%s", i.getUrl()))
                .orElseThrow(() -> new RuntimeException("Cannot determine Caching service from Discovery"));
        }
        clearSsl();
    }

    static Stream<Arguments> publicUrls() {
        return Stream.of(
            Arguments.of(INFO_PATH),
            Arguments.of(APIDOC_PATH)
        );
    }

    @Nested
    @NotAttlsTest
    class WhenCalledWithInvalidAuthentication {

        // Candidates for parametrized test.
        @ParameterizedTest
        @MethodSource("org.zowe.apiml.functional.caching.CachingAuthenticationTest#publicUrls")
        void publicEndpointIsAccessible(String endpoint) {
            given()
                .when()
                .get(caching_url + endpoint)
                .then()
                .statusCode(HttpStatus.OK.value());
        }

        @Test
        void givenUntrustedCert_cachingApiEndpointsAreInaccessible() {
            given()
                .config(SslContext.selfSignedUntrusted)
                .header(CERT_HEADER_NAME, "value")
                .when()
                .get(caching_url + CACHING_PATH)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void givenNoCertificateAndHeader_cachingApiEndpointsAreInaccessible() {
            given()
                .header(CERT_HEADER_NAME, "value")
                .when()
                .get(caching_url + CACHING_PATH)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void givenCertificateButNoHeader_cachingApiEndpointsAreInaccessible() {

            given()
                .config(SslContext.clientCertApiml)
                .when()
                .get(caching_url + CACHING_PATH)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        void givenNoCertificateAndNoHeader_cachingApiEndpointsAreInaccessible() {
            given()
                .when()
                .get(caching_url + CACHING_PATH)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    class WhenCalledWithValidAuthentication {
        @Test
        void cachingApiEndpointsAccessible() {
            given()
                .config(SslContext.clientCertApiml)
                .header(CERT_HEADER_NAME, "value")
                .when()
                .get(caching_url + CACHING_PATH)
                .then()
                .statusCode(HttpStatus.OK.value());
        }
    }


    private void clearSsl() {
        RestAssured.config = RestAssured.config().sslConfig(SSLConfig.sslConfig());
        RestAssured.useRelaxedHTTPSValidation();
    }
}
