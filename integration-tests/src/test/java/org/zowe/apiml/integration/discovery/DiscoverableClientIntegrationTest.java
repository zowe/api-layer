/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.discovery;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.RegistrationTest;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.zowe.apiml.util.requests.Endpoints.MEDIATION_CLIENT;

/**
 * Integration of
 */
@DiscoverableClientDependentTest
@RegistrationTest
class DiscoverableClientIntegrationTest implements TestWithStartedInstances {
    private static final URI MEDIATION_CLIENT_URI = HttpRequestUtils.getUriFromGateway(MEDIATION_CLIENT);

    @BeforeAll
    static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class WhenIntegratingWithDiscoveryService {
        @Nested
        class GivenValidService {
            @Test
            void verifyRegistrationAndUnregistration() {
                isRegistered(false, MEDIATION_CLIENT_URI);

                register(MEDIATION_CLIENT_URI);
                isRegistered(true, MEDIATION_CLIENT_URI);

                unregister(MEDIATION_CLIENT_URI);
                isRegistered(false, MEDIATION_CLIENT_URI);
            }
        }
    }

    private void isRegistered(boolean expectedRegistrationState, URI uri) {
        // It can take some time for (un)registration to complete
        await()
            .atMost(5, MINUTES)
            .pollDelay(0, SECONDS)
            .pollInterval(1, SECONDS)
            .until(() -> registeredStateAsExpected(expectedRegistrationState, uri));
    }

    private boolean registeredStateAsExpected(boolean expectedRegistrationState, URI uri) {
        try {
            given()
                .when()
                .get(uri)
                .then()
                .statusCode(is(SC_OK))
                .body("isRegistered", is(expectedRegistrationState));
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    private void register(URI uri) {
        given()
            .when()
            .post(uri)
            .then()
            .statusCode(is(SC_OK));
    }

    private void unregister(URI uri) {
        given()
            .when()
            .delete(uri)
            .then()
            .statusCode(is(SC_OK));
    }
}
