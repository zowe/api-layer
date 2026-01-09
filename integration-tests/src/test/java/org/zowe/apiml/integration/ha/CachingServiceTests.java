/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.ha;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.restassured.RestAssured;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.zowe.apiml.util.categories.ChaoticHATest;
import org.zowe.apiml.util.categories.HATest;
import org.zowe.apiml.util.config.*;
import org.zowe.apiml.util.requests.Endpoints;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.zowe.apiml.util.config.ConfigReader.environmentConfiguration;

@Slf4j
@HATest
@ChaoticHATest
@TestInstance(TestInstance.Lifecycle. PER_CLASS)
class CachingServiceTests {

    private static final boolean IS_MODULITH_ENABLED = Boolean.getBoolean("environment.modulith");
    private static final String SERVLET_PATH = IS_MODULITH_ENABLED ? "" : "/cachingservice";

    private static final String SERVICE = "service";
    private static final String KEY = "aCacheKey" + new Random().nextInt();
    private static final String VALUE = "aCacheValue";
    private static final String MAP = "aMap";
    private static final String MAP_KEY = "aMapCacheKey" + new Random().nextInt();
    private static final String MAP_VALUE = "aMapCacheValue";
    private static final String DN = "CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, ST=Prague, C=CZ";

    private static final KeyValue KEY_VALUE = new KeyValue(KEY, VALUE);
    private static final KeyValue MAP_KEY_VALUE = new KeyValue(MAP_KEY, MAP_VALUE);

    private Credentials credentials;
    private List<String> baseUrls;

    private boolean isUp(int index) {
        try {
            String url = String.format("%s%s%s", baseUrls.get(index), SERVLET_PATH, Endpoints.HEALTH);
            log.info("Check if {}. Caching Service is up: {}", index + 1, url);

            //@formatter:off
            given()
                .contentType(JSON)
                .auth()
                .basic(credentials.getUser(), credentials.getPassword())
            .when()
                .get(url)
            .then()
                .statusCode(200)
                .body("status", Matchers.is("UP"));
            //@formatter:on
            return true;
        } catch (Throwable t) {
            log.info("Caching service is down", t);
            return false;
        }
    }

    private boolean isUp() {
        for (int i = 0; i < baseUrls.size(); i++) {
            if (!isUp(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean isChaotic() {
        return Boolean.getBoolean("environment.chaotic");
    }

    @BeforeAll
    void setUp() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());

        EnvironmentConfiguration environmentConfiguration = environmentConfiguration();
        CachingServiceConfiguration cachingServiceConfiguration = environmentConfiguration.getCachingServiceConfiguration();

        KEY_VALUE.setServiceId(SERVICE);

        assumeTrue(cachingServiceConfiguration.getHost() != null);
        baseUrls = Arrays.stream(cachingServiceConfiguration.getHost().split("[,;]"))
            .map(host -> String.format("%s://%s:%d", cachingServiceConfiguration.getScheme(), host, cachingServiceConfiguration.getPort()))
            .collect(Collectors.toList());
        assumeTrue(baseUrls.size() > 1, "This test requires multiple instances of Caching service.");
        credentials = ConfigReader.environmentConfiguration().getCredentials();

        await()
            .atMost(10, MINUTES)
            .pollDelay(0, SECONDS)
            .pollInterval(10, SECONDS)
            .until(this::isUp);
    }

    private void assertContent(int index) {
        //@formatter:off

        // check the all records (tokenCache)
        given()
            .config(SslContext.clientCertApiml)
            .header("X-Certificate-DistinguishedName", DN)
            .when()
            .get(baseUrls.get(index) + "/cachingservice/api/v1/cache-list")
            .then()
            .statusCode(200)
            .body(MAP + "." + MAP_KEY, equalTo(MAP_VALUE));

        // check the all records (tokenCache)
        given()
            .config(SslContext.clientCertApiml)
            .header("X-Certificate-DistinguishedName", DN)
            .when()
            .get(baseUrls.get(index) + "/cachingservice/api/v1/cache-list/" + MAP)
            .then()
            .statusCode(200)
            .body(MAP_KEY, equalTo(MAP_VALUE));

        // check the concrete record (cache)
        given()
            .config(SslContext.clientCertApiml)
            .header("X-Certificate-DistinguishedName", DN)
            .when()
            .get(baseUrls.get(index) + "/cachingservice/api/v1/cache/" + KEY)
            .then()
            .statusCode(200)
            .body("value", equalTo(VALUE));

        //@formatter:on
    }

    @Test
    void givenMultipleInstances_whenShareAValue_thenShutdownDoesntChangeTheState() {
        log.info("Set value on the first instance to cache storage");
        //@formatter:off
        given()
            .config(SslContext.clientCertApiml)
            .header("X-Certificate-DistinguishedName", DN)
            .contentType(JSON)
            .body(KEY_VALUE)
            .when()
            .post(baseUrls.get(0) + "/cachingservice/api/v1/cache")
            .then()
            .statusCode(201);

        log.info("Set value on the first instance to tokenCache storage");
        given()
            .config(SslContext.clientCertApiml)
            .header("X-Certificate-DistinguishedName", DN)
            .contentType(JSON)
            .body(MAP_KEY_VALUE)
            .when()
            .post(baseUrls.get(0) + "/cachingservice/api/v1/cache-list/" + MAP)
            .then()
            .statusCode(201);
        //@formatter:on

        int instances = baseUrls.size();
        for (int i = -1; i < instances - 1; i++) {
            if (i >= 0) {
                if (!isChaotic()) {
                    // for non-chaotic stop the test once all instances are verified
                    return;
                }

                log.info("Kill {}. instance of caching service", i + 1);
                //@formatter:off
                given()
                    .config(SslContext.clientCertApiml)
                    .contentType(JSON)
                    .auth().basic(credentials.getUser(), credentials.getPassword())
                    .when()
                    .post(baseUrls.get(i) + SERVLET_PATH + "/application/shutdown")
                    .then()
                    .statusCode(is(SC_OK));
                //@formatter:on
            }

            for (int j = i + 1; j < instances; j++) {
                log.info("Check if the value is accessible {}. instance", j + 1);
                assertContent(j);
            }
        }

    }

    @RequiredArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Data
    static class KeyValue implements Serializable {

        private final String key;
        private final String value;
        private String serviceId;
        private final String created;

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
            this.serviceId = "";
            this.created = currentTime();
        }

        private static String currentTime() {
            return String.valueOf(new Date().getTime());
        }

        @JsonCreator
        public KeyValue() {
            key = "";
            value = "";
            serviceId = "";
            created = currentTime();
        }

    }

}
