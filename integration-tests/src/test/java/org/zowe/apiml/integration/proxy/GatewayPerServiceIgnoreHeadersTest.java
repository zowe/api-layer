/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.proxy;

import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.http.HttpRequestUtils;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

@DiscoverableClientDependentTest
public class GatewayPerServiceIgnoreHeadersTest implements TestWithStartedInstances {
    private static final String HEADER = "myheader";
    private static final String HEADER_VALUE = "myheadervalue";
    private static final String REQUEST_INFO_ENDPOINT = "/discoverableclient/api/v1/request";

    @BeforeAll
    static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class GivenServiceWithIgnoredHeaders {
        @Test
        void whenIncludeIgnoredHeader_thenThatHeaderIsIgnored() {
            given()
                .header(HEADER, HEADER_VALUE)
                .when()
                .get(HttpRequestUtils.getUriFromGateway(REQUEST_INFO_ENDPOINT))
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("headers." + HEADER, is(nullValue()));
        }

        @Test
        void whenIncludeUnignoredHeader_thenThatHeaderIsIncluded() {
            String includedHeader = HEADER + "unignored";
            given()
                .header(includedHeader, HEADER_VALUE)
                .when()
                .get(HttpRequestUtils.getUriFromGateway(REQUEST_INFO_ENDPOINT))
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("headers." + includedHeader, is(HEADER_VALUE));
        }
    }
}
