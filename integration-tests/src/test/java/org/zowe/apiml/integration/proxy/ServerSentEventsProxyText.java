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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

@TestsNotMeantForZowe
public class ServerSentEventsProxyText {

    @BeforeAll
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class WhenRoutingSession {
        @ParameterizedTest(name = "WhenRoutingSession.thenReturnEvents#message {0}")
        @ValueSource(strings = {"/discoverableclient/sse/v1/events", "/sse/v1/discoverableclient/events"})
        void thenReturnEvents(String path) {
            URI uri = HttpRequestUtils.getUriFromGateway(path);

            given()
                .when()
                .get(uri)
                .then()
                .statusCode(is(HttpStatus.SC_OK))
                .content(is("data:event"))
                .header("Content-Type", "text/event-stream");
        }

        // incorrect route
        // multiple connections at once?
        // authentication?
    }
}
