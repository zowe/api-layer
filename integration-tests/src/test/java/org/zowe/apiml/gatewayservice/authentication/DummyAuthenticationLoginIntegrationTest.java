/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice.authentication;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

public class DummyAuthenticationLoginIntegrationTest extends Login {
    @BeforeAll
    static void switchToTestedProvider() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();

        currentProvider = loadCurrentProvider();
        switchProvider("dummy");
    }
}
