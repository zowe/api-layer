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
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

public class Requests {
    public ReadContext getJson(URI uri) {
        String apps = given()
            .accept(ContentType.JSON)
        .when()
            .get(uri)
        .then()
            .statusCode(is(HttpStatus.SC_OK))
            .extract()
            .body()
            .toString();

        return JsonPath.parse(apps);
    }

    public JsonResponse getJsonResponse(URI uri) {
        ExtractableResponse<Response> response = given()
            .accept(ContentType.JSON)
        .when()
            .get(uri)
            .then().extract();

        ReadContext context = JsonPath.parse(response.body().toString());
        return new JsonResponse(response.statusCode(), context);
    }
}
