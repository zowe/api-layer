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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.RegistrationTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.DiscoverableClientConfiguration;
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
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.zowe.apiml.util.requests.Endpoints.MEDIATION_CLIENT;

@DiscoverableClientDependentTest
@RegistrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiscoverableClientIntegrationTest implements TestWithStartedInstances {

    private static final URI MEDIATION_CLIENT_URI = HttpRequestUtils.getUriFromGateway(MEDIATION_CLIENT);
    private DiscoverableClientConfiguration discoverableClientConfig;

    @BeforeAll
    void beforeClass() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
        discoverableClientConfig = ConfigReader.environmentConfiguration().getDiscoverableClientConfiguration();
    }

    @Nested
    class WhenIntegratingWithDiscoveryService {

        @BeforeEach
        void setUp() {
            unregister(MEDIATION_CLIENT_URI);
        }

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

                    register(MEDIATION_CLIENT_URI, Collections.singletonMap("apiml.gatewayUrl", "http://www.invalid.com"))
                        .and()
                        .statusCode(is(SC_OK));

                    isRegistered(false, MEDIATION_CLIENT_URI);
                }

                // update some metadata URL
                @Test
                void whenUpdateMetadataWithUrlNotInAllowList_thenReject() {
                    isRegistered(false, MEDIATION_CLIENT_URI);

                    register(MEDIATION_CLIENT_URI, Collections.emptyMap()).and()
                        .statusCode(is(SC_OK));
                    isRegistered(true, MEDIATION_CLIENT_URI);

                    String instanceId = discoverableClientConfig.getHost() + ":registrationTest:10013";

                    given()
                        .config(SslContext.clientCertValid)
                        .contentType(ContentType.JSON)
                    .when()
                        .put(DiscoveryUtils.getDiscoveryUrl() + String.format("/eureka/apps/REGISTRATIONTEST/%s/metadata?apiml.externalUrl=https://baddomain.net", instanceId))
                    .then()
                        .statusCode(is(SC_INTERNAL_SERVER_ERROR));

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
                    .when()
                        .put(DiscoveryUtils.getDiscoveryUrl() + String.format("/eureka/apps/REGISTRATIONTEST/%s/metadata?apiml.externalUrl=https://www.zowe.org", discoverableClientConfig.getHost() + ":registrationTest:10013" ))
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
            .contentType(ContentType.JSON)
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
