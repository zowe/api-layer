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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.zOSMFAuthTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;

/**
 * This test suite verifies integration between the API ML and the service instance. Especially it verifies that gateway
 * properly accepts and translates the token.
 * The expectation is that the call to the service instance will be authenticated.
 * The tests depends on the availability of either the zOSMF or the mock.
 */
@zOSMFAuthTest
class ServiceIntegrationTest implements TestWithStartedInstances {
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String ZOSMF_SERVICE_ID = ConfigReader.environmentConfiguration().getZosmfServiceConfiguration().getServiceId();
    private final static String BASE_PATH = "/api/" + ZOSMF_SERVICE_ID;
    private final static String ZOSMF_ENDPOINT = "/zosmf/restfiles/ds?dslevel=sys1.p*";

    private String token;
    private String scheme;
    private String host;
    private int port;

    @BeforeEach
    void setUp() {
        GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        scheme = serviceConfiguration.getScheme();
        host = serviceConfiguration.getHost();
        port = serviceConfiguration.getPort();

        RestAssured.port = port;
        RestAssured.basePath = BASE_PATH;
        RestAssured.useRelaxedHTTPSValidation();
        token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
    }

    @Nested
    class GivenValidToken {
        @Test
            //@formatter:off
        void authenticateViaBearerHeader() {
            String dsname1 = "SYS1.PARMLIB";
            String dsname2 = "SYS1.PROCLIB";

            given()
                .header("Authorization", "Bearer " + token)
                .header("X-CSRF-ZOSMF-HEADER", "zosmf")
                .when()
                .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
                .then()
                .statusCode(is(SC_OK))
                .body(
                    "items.dsname", hasItems(dsname1, dsname2));
        }

        @Test
        void authenticateViaCookie() {
            String dsname1 = "SYS1.PARMLIB";
            String dsname2 = "SYS1.PROCLIB";

            given()
                .cookie("apimlAuthenticationToken", token)
                .header("X-CSRF-ZOSMF-HEADER", "zosmf")
                .when()
                .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
                .then()
                .statusCode(is(SC_OK))
                .body(
                    "items.dsname", hasItems(dsname1, dsname2));
        }

        @Test
        void authenticateViaBasicAuthHeader() {
            String dsname1 = "SYS1.PARMLIB";
            String dsname2 = "SYS1.PROCLIB";

            given()
                .auth().preemptive().basic(USERNAME, PASSWORD)
                .header("X-CSRF-ZOSMF-HEADER", "zosmf")
                .when()
                .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
                .then()
                .statusCode(is(SC_OK))
                .body(
                    "items.dsname", hasItems(dsname1, dsname2));
        }
    }

    @Nested
    class GivenInvalidToken_RejectAuthentication {

        @Test
        void viaBearerHeader() {
            String invalidToken = "token";

            given()
                .header("Authorization", "Bearer " + invalidToken)
                .header("X-CSRF-ZOSMF-HEADER", "zosmf")
                .when()
                .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
                .then()
                .statusCode(is(SC_UNAUTHORIZED));
        }

        @Test
        void viaApimlCookie() {
            String invalidToken = "token";

            given()
                .cookie("apimlAuthenticationToken", invalidToken)
                .header("X-CSRF-ZOSMF-HEADER", "zosmf")
                .when()
                .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
                .then()
                .statusCode(is(SC_UNAUTHORIZED));
        }
    }

    @Nested
    class GivenNoAuthentication_RejectAuthentication {
        @Test
        void withoutAnyAuthenticationMethod() {
            given()
                .header("X-CSRF-ZOSMF-HEADER", "zosmf")
                .when()
                .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
                .then()
                .statusCode(is(SC_UNAUTHORIZED));
        }

        @Test
        void withEmptyHeader() {
            String emptyToken = " ";

            given()
                .header("Authorization", "Bearer " + emptyToken)
                .header("X-CSRF-ZOSMF-HEADER", "zosmf")
                .when()
                .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
                .then()
                .statusCode(is(SC_UNAUTHORIZED));
        }

        @Test
        void withEmptyCookie() {
            String emptyToken = "";

            given()
                .cookie("apimlAuthenticationToken", emptyToken)
                .header("X-CSRF-ZOSMF-HEADER", "zosmf")
                .when()
                .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
                .then()
                .statusCode(is(SC_UNAUTHORIZED));
        }
    }
    //@formatter:on
}
