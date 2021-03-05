/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util;

import io.restassured.config.RestAssuredConfig;
import org.zowe.apiml.cachingservice.KeyValue;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.hamcrest.core.Is.is;

public class CachingRequests {
    private static final URI CACHING_PATH = HttpRequestUtils.getUriFromGateway("/cachingservice/api/v1/cache");

    public void create(KeyValue keyValue, RestAssuredConfig config) {
        create(CACHING_PATH, keyValue, config);
    }

    public void create(URI cachingPath, KeyValue keyValue, RestAssuredConfig config) {
        given().config(config)
            .contentType(JSON)
            .body(keyValue)
            .when()
            .post(cachingPath)
            .then()
            .statusCode(is(SC_CREATED));
    }

    public void deleteValueUnderServiceIdWithoutValidation(String value, RestAssuredConfig config) {
        deleteValueUnderServiceIdWithoutValidation(CACHING_PATH, value, config);
    }

    public void deleteValueUnderServiceIdWithoutValidation(URI cachingPath, String value, RestAssuredConfig config) {
        given().config(config)
            .contentType(JSON)
            .when()
            .delete(cachingPath + "/" + value);
    }
}
