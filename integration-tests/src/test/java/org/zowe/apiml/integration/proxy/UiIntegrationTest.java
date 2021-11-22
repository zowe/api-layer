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
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import static io.restassured.RestAssured.given;
import static org.zowe.apiml.util.requests.Endpoints.*;

@DiscoverableClientDependentTest
class UiIntegrationTest implements TestWithStartedInstances {
    @Nested
    class WhenCallingUiRoute {
        @Nested
        class GivenUiUrl {
            @Test
            @TestsNotMeantForZowe
            void returnUi() {
                given()
                .when()
                    .get(HttpRequestUtils.getUriFromGateway(STATIC_UI + "/"))
                .then()
                    .statusCode(HttpStatus.SC_OK);
            }
        }

        @Nested
        class GivenRedirectUrl {
            @Test
            @TestsNotMeantForZowe
            void returnRedirect() {
                given()
                .when()
                    .get(HttpRequestUtils.getUriFromGateway(STATIC_UI))
                .then()
                    .statusCode(HttpStatus.SC_OK);
            }
        }
    }
}
