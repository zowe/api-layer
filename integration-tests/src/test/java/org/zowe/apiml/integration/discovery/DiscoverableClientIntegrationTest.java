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
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.RegistrationTest;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.service.DiscoveryUtils;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.zowe.apiml.util.requests.Endpoints.MEDIATION_CLIENT;

@DiscoverableClientDependentTest // TODO This does not run on z/OS tests
@RegistrationTest // TODO Runs in GA as CITestsRegistration, add CITestsRegistrationModulith
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiscoverableClientIntegrationTest implements TestWithStartedInstances {

    private static final URI MEDIATION_CLIENT_URI = HttpRequestUtils.getUriFromGateway(MEDIATION_CLIENT);

    @BeforeAll
    void beforeClass() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
    }

    @Nested
    class WhenIntegratingWithDiscoveryService {

        @Nested
        class GivenValidService {

            /*
            This test relies on a DiscoverableClient endpoint to trigger API ML registration and unregistration.
            This one works also as a positive test case for the allowlist (same as normal running on the services)
            */
            @Test
            void verifyRegistrationAndUnregistration() {
                isRegistered(false, MEDIATION_CLIENT_URI);

                register(MEDIATION_CLIENT_URI, Collections.emptyMap()).and()
                    .statusCode(is(SC_OK));
                isRegistered(true, MEDIATION_CLIENT_URI);

                unregister(MEDIATION_CLIENT_URI);
                isRegistered(false, MEDIATION_CLIENT_URI);
            }

            /**
             * Tests to directly call Discovery Service endpoints to register and update metadata of a service.
             */
            @Nested
            class GivenUrlsNotInAllowList {

                // register with invalid URL
                @Test
                void whenRegisteringWithUrlNotInAllowList_thenReject() {
                    isRegistered(false, MEDIATION_CLIENT_URI);

                    register(MEDIATION_CLIENT_URI, Collections.singletonMap("some.url", "http://invalid.com"))
                        .and()
                        .statusCode(is(SC_FORBIDDEN));

                    isRegistered(false, MEDIATION_CLIENT_URI);
                }

                // update some metadata URL
                @Test
                void whenUpdateMetadataWithUrlNotInAllowList_thenReject() {
                    isRegistered(false, MEDIATION_CLIENT_URI);

                    register(MEDIATION_CLIENT_URI, Collections.emptyMap()).and()
                        .statusCode(is(SC_OK));
                    isRegistered(true, MEDIATION_CLIENT_URI);

                    given()
                        .config(SslContext.clientCertValid)
                        .contentType(ContentType.JSON)
                    .when() // FIXME find InstanceID and confirm serviceID
                        .put(DiscoveryUtils.getDiscoveryUrl() + "/eureka/v2/apps/registrationtest/instanceID/metadata?some.other.url=https://baddomain.net")
                    .then()
                        .statusCode(is(SC_FORBIDDEN));

                    unregister(MEDIATION_CLIENT_URI);
                    isRegistered(false, MEDIATION_CLIENT_URI);
                }

                @Test
                void whenUpdateMetadataWithUrlInAllowList_thenAllow() {
                    isRegistered(false, MEDIATION_CLIENT_URI);

                    register(MEDIATION_CLIENT_URI, Collections.emptyMap()).and()
                        .statusCode(is(SC_OK));
                    isRegistered(true, MEDIATION_CLIENT_URI);

                    given()
                        .config(SslContext.clientCertValid)
                        .contentType(ContentType.JSON)
                    .when() //FIXME find InstanceID and confirm serviceID
                        .put(DiscoveryUtils.getDiscoveryUrl() + "/eureka/v2/apps/registrationtest/instanceID/metadata?key=value")
                    .then()
                        .statusCode(is(SC_OK));

                    unregister(MEDIATION_CLIENT_URI);
                    isRegistered(false, MEDIATION_CLIENT_URI);
                }

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

    private ValidatableResponse register(URI uri, Map<String, Object> additionalMetadata) {
        return given()
            .body(additionalMetadata == null ? Collections.emptyMap() : additionalMetadata)
        .when()
            .post(uri)
        .then();
    }

    private void unregister(URI uri) {
        given()
        .when()
            .delete(uri)
        .then()
            .statusCode(is(SC_OK));
    }

}
