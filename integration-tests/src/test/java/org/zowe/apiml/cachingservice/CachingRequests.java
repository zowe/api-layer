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

import org.zowe.apiml.gatewayservice.SecurityUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.hamcrest.core.Is.is;

public class CachingRequests {
    private static final URI CACHING_PATH = HttpRequestUtils.getUriFromGateway("/cachingservice/api/v1/cache");
    private final static String COOKIE_NAME = "apimlAuthenticationToken";
    private static String jwtToken = SecurityUtils.gatewayToken();


    public void create(KeyValue keyValue) {
        given()
            .contentType(JSON)
            .body(keyValue)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .post(CACHING_PATH)
            .then()
            .statusCode(is(SC_CREATED));
    }

    public void deteleValueUnderServiceIdWithoutValidation(String value, String jwtToken) {
        given()
            .contentType(JSON)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .delete(CACHING_PATH + "/" + value);
    }
}
