/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.authentication;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.zowe.apiml.util.categories.zOSMFAuthTest;

import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

@zOSMFAuthTest
public class ZosmfQueryTest extends QueryTest {
    @BeforeAll
    static void switchToTestedProvider() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }
}
