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
import org.junit.jupiter.api.*;
import org.zowe.apiml.util.CachingRequests;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.NotForMainframeTest;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.service.RunningService;
import org.zowe.apiml.util.service.ServiceJars;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.hamcrest.core.Is.is;

@NotForMainframeTest
@Disabled
class RemoveOldestTest implements TestWithStartedInstances {
    private final CachingRequests requests = new CachingRequests();
    private static Map<String, String> parameters = new HashMap<>();
    private static final int numberOfRecords = 2;
    private static final RunningService cachingServiceInstance;
    static {
        parameters.put("-Dcaching.storage.evictionStrategy", "removeOldest");
        parameters.put("-Dapiml.service.serviceId", "cachingoldest");
        parameters.put("-Dapiml.service.port", "10023");
        parameters.put("-Dcaching.storage.size", String.valueOf(numberOfRecords));
        cachingServiceInstance = new RunningService("cachingoldest",
            ServiceJars.CACHING, parameters, new HashMap<>());
    }

    @BeforeAll
    static void setup() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication();
        cachingServiceInstance.start();
        cachingServiceInstance.waitUntilReady();
    }

    @AfterAll
    static void tearDown() {
        cachingServiceInstance.stop();
    }

    @Test
    void givenStorageIsFull_whenAnotherKeyIsInserted_thenTheOldestIsRemoved() {
        int amountOfAllowedRecords = numberOfRecords + 5;
        URI removeOldestCachingServiceUri = HttpRequestUtils.getUriFromGateway("/cachingoldest/api/v1/cache");

        KeyValue keyValue;

        for (int i = 0; i < amountOfAllowedRecords; i++) {
            keyValue = new KeyValue("key" + i, "testValue");
            requests.create(removeOldestCachingServiceUri, keyValue, SslContext.clientCertValid);
        }

        given().config(SslContext.clientCertValid)
            .contentType(JSON)
            .when()
            .get(removeOldestCachingServiceUri + "/key0")
            .then()
            .statusCode(is(SC_NOT_FOUND));

    }
}
