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

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.zowe.apiml.util.SecurityUtils.COOKIE_NAME;

@Slf4j
public class AuthenticatedRequest {

    
    public Response execute(RequestParams params) {
        return executeRequest(params, createSpecification(params));
    }

    private Response executeRequest(RequestParams params, RequestSpecification response) {
        switch (params.getMethod()) {
            case GET:
                return response.get(params.getUri());
            case POST:
                return response.post(params.getUri());
            default:
                throw new RuntimeException("Not Implemented yet");
        }
    }

    private RequestSpecification createSpecification(RequestParams params) {
        RequestSpecification when;
        if (Objects.nonNull(params.getAuthentication())) {
            when = given()
                .accept(ContentType.JSON)
                .cookie(COOKIE_NAME, params.getAuthentication())
                .when();
        } else {
            when = given()
                .accept(ContentType.JSON)
                .when();
        }

        return when;
    }

}
