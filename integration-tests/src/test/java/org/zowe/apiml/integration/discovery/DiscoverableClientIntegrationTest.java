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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.SlowTests;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

@TestsNotMeantForZowe
@DiscoverableClientDependentTest
class DiscoverableClientIntegrationTest implements TestWithStartedInstances {
    private static final URI MEDIATION_CLIENT_URI = HttpRequestUtils.getUriFromGateway("/discoverableclient/api/v1/apiMediationClient");
    private static final URI MEDIATION_CLIENT_URI_OLD_FORMAT = HttpRequestUtils.getUriFromGateway("/api/v1/discoverableclient/apiMediationClient");

    @BeforeAll
    static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    protected static URI[] discoverableClientSource() {
        return new URI[]{MEDIATION_CLIENT_URI, MEDIATION_CLIENT_URI_OLD_FORMAT};
    }

    @ParameterizedTest
    @MethodSource("discoverableClientSource")
    void shouldBeUnregisteredBeforeRegistration(URI url) {
        requestIsRegistered(false, url);
    }

    @ParameterizedTest
    @MethodSource("discoverableClientSource")
    @SlowTests
    void shouldBeRegisteredAfterRegistration(URI url) {
        requestToRegister(url);
        requestIsRegistered(true, url);

        requestToUnregister(url);
    }

    @ParameterizedTest
    @MethodSource("discoverableClientSource")
    @SlowTests
    void shouldNotBeRegisteredAfterUnregistration(URI url) {
        requestToRegister(url);
        requestToUnregister(url);
    }

    private void requestIsRegistered(boolean expectedRegistrationState, URI uri) {
        // It can take some time for (un)registration to complete
        await().atMost(5, MINUTES).pollDelay(0, SECONDS).pollInterval(1, SECONDS)
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

    private void requestToRegister(URI uri) {
        given()
            .when()
            .post(uri)
            .then()
            .statusCode(is(SC_OK));
    }

    private void requestToUnregister(URI uri) {
        given()
            .when()
            .delete(uri)
            .then()
            .statusCode(is(SC_OK));
    }
}
