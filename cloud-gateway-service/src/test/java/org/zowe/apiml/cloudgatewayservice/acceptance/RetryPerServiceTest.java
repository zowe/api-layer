/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance;

import com.sun.net.httpserver.Headers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTest;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTestWithTwoServices;

import javax.ws.rs.HEAD;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@AcceptanceTest
class RetryPerServiceTest extends AcceptanceTestWithTwoServices {

    private static final String HEADER_X_FORWARD_TO = "X-Forward-To";

    Consumer<Headers> dummyConsumer = (headers -> {
    });

    @Nested
    class GivenRetryOnAllOperationsIsDisabled {
        @Test
        void whenGetReturnsUnavailable_thenRetry() throws Exception {
            AtomicInteger counter = mockServerWithSpecificHttpResponse(503, "/serviceid2/test", 0, dummyConsumer, "".getBytes());
            given()
                .header(HEADER_X_FORWARD_TO, "serviceid2")
                .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_SERVICE_UNAVAILABLE));
            assertEquals(6, counter.get());
        }

        @Test
        void whenRequestReturnsUnauthorized_thenDontRetry() throws Exception {
            AtomicInteger counter = mockServerWithSpecificHttpResponse(401, "/serviceid2/test", 0, dummyConsumer, "".getBytes());
            given()
                .header(HEADER_X_FORWARD_TO, "serviceid2")
                .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_UNAUTHORIZED));
            assertEquals(1, counter.get());
            given()
                .header(HEADER_X_FORWARD_TO, "serviceid2")
                .when()
                .post(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_UNAUTHORIZED));
            assertEquals(2, counter.get());
            given()
                .header(HEADER_X_FORWARD_TO, "serviceid2")
                .when()
                .put(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_UNAUTHORIZED));
            assertEquals(3, counter.get());
            given()
                .header(HEADER_X_FORWARD_TO, "serviceid2")
                .when()
                .delete(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_UNAUTHORIZED));
            assertEquals(4, counter.get());
            given()
                .header(HEADER_X_FORWARD_TO, "serviceid2")
                .when()
                .patch(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_UNAUTHORIZED));
            assertEquals(5, counter.get());
        }

        @Test
        void whenPostReturnsUnavailable_thenDontRetry() throws Exception {
            AtomicInteger counter = mockServerWithSpecificHttpResponse(503, "/serviceid2/test", 0, dummyConsumer, "".getBytes());
            given()
                .header(HEADER_X_FORWARD_TO, "serviceid2")
                .when()
                .post(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_SERVICE_UNAVAILABLE));
            assertEquals(1, counter.get());
        }

    }

}
