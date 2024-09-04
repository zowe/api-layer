/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util.requests;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

import java.net.URI;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.SecurityUtils.COOKIE_NAME;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

@Slf4j
public class Requests {
    public ReadContext getJson(URI uri) {
        String apps = given()
            .config(RestAssuredConfig.config().sslConfig(getConfiguredSslConfig()))
            .accept(ContentType.JSON)
        .when()
            .get(uri)
        .then()
            .statusCode(is(HttpStatus.SC_OK))
            .extract()
            .body()
            .asString();

        return JsonPath.parse(apps);
    }

    public JsonResponse getJsonResponse(URI uri) {
        return getJsonResponse(uri, "", COOKIE_NAME);
    }

    public JsonResponse getJsonResponse(URI uri, String authentication, String cookieName) {
        ExtractableResponse<Response> response = given()
            .accept(ContentType.JSON)
            .cookie(cookieName, authentication)
        .when()
            .get(uri)
        .then().extract();

        String responseText = response.body().asString();
        Map<String, String> cookies = response.cookies();
        ReadContext context = JsonPath.parse(responseText);
        log.info("Response: {}, Response Code: {}", responseText, response.statusCode());
        return new JsonResponse(response.statusCode(), context, cookies);
    }
}
