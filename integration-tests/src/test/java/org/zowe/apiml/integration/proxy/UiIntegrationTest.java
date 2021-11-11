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

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import static io.restassured.RestAssured.given;

@DiscoverableClientDependentTest
class UiIntegrationTest implements TestWithStartedInstances {
    protected static String[] discoverableClientSource() {
        return new String[]{
            "/discoverableclient/ui/v1",
            "/ui/v1/discoverableclient"
        };
    }

    @Nested
    class WhenCallingUiRoute {
        @Nested
        class GivenUiUrl {
            @ParameterizedTest(name = "returnUi {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.proxy.UiIntegrationTest#discoverableClientSource")
            @TestsNotMeantForZowe
            void returnUi(String url) {
                given()
                .when()
                    .get(HttpRequestUtils.getUriFromGateway(url + "/"))
                .then()
                    .statusCode(HttpStatus.SC_OK);
            }
        }

        @Nested
        class GivenRedirectUrl {
            @ParameterizedTest(name = "returnRedirect {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.proxy.UiIntegrationTest#discoverableClientSource")
            @TestsNotMeantForZowe
            void returnRedirect(String url) {
                given()
                .when()
                    .get(HttpRequestUtils.getUriFromGateway(url))
                .then()
                    .statusCode(HttpStatus.SC_OK);
            }
        }
    }
}
