/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discoverableclient;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.SlowTests;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

class IntegratedApiMediationClientTest {
    private static final URI MEDIATION_CLIENT_URI = HttpRequestUtils.getUriFromGateway("/api/v1/discoverableclient/apiMediationClient");

    @BeforeAll
    static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void shouldBeUnregisteredBeforeRegistration() {
        requestIsRegistered(false);
    }

    @Test
    @SlowTests
    void shouldBeRegisteredAfterRegistration() {
        requestToRegister();
        requestIsRegistered(true);

        requestToUnregister();
    }

    @Test
    @SlowTests
    void shouldNotBeRegisteredAfterUnregistration() {
        requestToRegister();
        requestToUnregister();
    }

    private void requestIsRegistered(boolean expectedRegistrationState) {
        // It can take some time for (un)registration to complete
        await().atMost(5, MINUTES).pollDelay(0, SECONDS).pollInterval(1, SECONDS)
            .until(() -> registeredStateAsExpected(expectedRegistrationState));
    }

    private boolean registeredStateAsExpected(boolean expectedRegistrationState) {
        try {
            given()
            .when()
                .get(MEDIATION_CLIENT_URI)
            .then()
                .statusCode(is(SC_OK))
                .body("isRegistered", is(expectedRegistrationState));
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    private void requestToRegister() {
        given()
        .when()
            .post(MEDIATION_CLIENT_URI)
        .then()
            .statusCode(is(SC_OK));
    }

    private void requestToUnregister() {
        given()
        .when()
            .delete(MEDIATION_CLIENT_URI)
        .then()
            .statusCode(is(SC_OK));
    }
}
