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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gatewayservice.SecurityUtils;
import org.zowe.apiml.util.CachingRequests;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_INSUFFICIENT_STORAGE;
import static org.hamcrest.core.Is.is;

//@NotForMainframeTest // Remove later when implemented for VSAM as well.
class RejectEvictionTest {
    private static final URI CACHING_PATH = HttpRequestUtils.getUriFromGateway("/cachingservice/api/v1/cache");
    private final String COOKIE_NAME = "apimlAuthenticationToken";
    private final String jwtToken = SecurityUtils.gatewayToken();
    private final CachingRequests requests = new CachingRequests();

    @BeforeAll
    static void setup() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void givenStorageIsFull_whenAnotherKeyIsInserted_thenItIsRejected() {
        int amountOfAllowedRecords = 100;
        try {
            KeyValue keyValue;

            // The default configuration is to allow 100 records.
            for (int i = 0; i < amountOfAllowedRecords; i++) {
                keyValue = new KeyValue("key" + i, "testValue");
                requests.create(keyValue);
            }

            keyValue = new KeyValue("keyThatWontPass", "testValue");
            given()
                .contentType(JSON)
                .body(keyValue)
                .cookie(COOKIE_NAME, jwtToken)
            .when()
                .post(CACHING_PATH)
            .then()
                .statusCode(is(SC_INSUFFICIENT_STORAGE));
        } finally {
            for (int i = 0; i < amountOfAllowedRecords; i++) {
                requests.deleteValueUnderServiceIdWithoutValidation("key" + i, jwtToken);
            }
        }
    }
}
