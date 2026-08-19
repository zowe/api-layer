/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance.corsTests;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@AcceptanceTest
@TestPropertySource(properties = {
    "apiml.service.corsEnabled=false",
    "apiml.service.corsDefaultAllowedOrigins="
})
class CorsDisabledTest extends AcceptanceTestWithTwoServices {

    @Test
    void whenSimpleCorsRequestArrives_thenReject() throws Exception {
        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
        mockValid200HttpResponse();
        discoveryClient.createRefreshCacheEvent();

        given()
            .header(new Header("Origin", "https://foo.bar.org"))
            .contentType(ContentType.TEXT)
            .log().ifValidationFails()
            .when()
            .get(basePath + serviceWithCustomConfiguration.getPath())
            .then()
            .log().ifValidationFails()
            .statusCode(is(SC_FORBIDDEN))
            .body(is("Invalid CORS request"));

        verify(mockClient, never()).execute(ArgumentMatchers.any(HttpUriRequest.class));
    }

    @Test
    void whenPreflightRequestArrives_thenReject() throws Exception {

        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
        mockValid200HttpResponse();
        discoveryClient.createRefreshCacheEvent();

        given()
            .header(new Header("Origin", "https://foo.bar.org"))
            .header(new Header("Access-Control-Request-Method", "POST"))
            .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
            .log().ifValidationFails()
            .when()
            .options(basePath + serviceWithCustomConfiguration.getPath())
            .then()
            .log().ifValidationFails()
            .statusCode(is(SC_FORBIDDEN))
            .body(is("Invalid CORS request"));

        verify(mockClient, never()).execute(ArgumentMatchers.any(HttpUriRequest.class));

    }

    @Test
    void whenSecFetchHeadersArrive_thenRejectBeforeCors() throws Exception {

        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
        mockValid200HttpResponse();
        discoveryClient.createRefreshCacheEvent();

        given()
            .header(new Header("Sec-Fetch-Site", "cross-site"))
            .log().ifValidationFails()
            .when()
            .post(basePath + serviceWithCustomConfiguration.getPath())
            .then()
            .log().ifValidationFails()
            .statusCode(is(SC_FORBIDDEN))
            .body(is("Access denied."));

        verify(mockClient, never()).execute(ArgumentMatchers.any(HttpUriRequest.class));

    }

}
