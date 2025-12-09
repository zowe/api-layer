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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.zowe.apiml.util.categories.ChaoticHATest;
import org.zowe.apiml.util.config.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.zowe.apiml.util.config.ConfigReader.environmentConfiguration;

@ChaoticHATest
@TestInstance(TestInstance.Lifecycle. PER_CLASS)
public class CachingService {

    private static final String SERVICE = "service";
    private static final String KEY = "aCacheKey" + new Random().nextInt();
    private static final String VALUE = "aCacheValue";

    private static final KeyValue KEY_VALUE = new KeyValue(KEY, VALUE);

    private Credentials credentials;
    private List<String> baseUrls;

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
        credentials = ConfigReader.environmentConfiguration().getCredentials();
    }

    @Test
    void testIt() {
        given()
            .config(SslContext.clientCertApiml)
            .header("X-Certificate-DistinguishedName", SERVICE)
            .contentType(JSON)
            .body(KEY_VALUE)
        .when()
            .post(baseUrls.get(0) + "/cachingservice/api/v1/cache")
        .then()
            .statusCode(201);

        int instances = baseUrls.size();
        for (int i = -1; i < instances - 1; i++) {
            if (i >= 0) {
                // kill caching service
                given()
                    .config(SslContext.clientCertApiml)
                    .contentType(JSON)
                    .auth().basic(credentials.getUser(), credentials.getPassword())
                .when()
                    .post(baseUrls.get(i) + "/cachingservice/application/shutdown")
                .then()
                    .statusCode(is(SC_OK));
            }

            for (int j = i + 1; j < instances; j++) {
                // check if the value is accessible
                given()
                    .config(SslContext.clientCertApiml)
                    .header("X-Certificate-DistinguishedName", SERVICE)
                .when()
                    .get(baseUrls.get(j) + "/cachingservice/api/v1/cache/" + KEY)
                .then()
                    .statusCode(200)
                    .body("value", equalTo(VALUE));
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
