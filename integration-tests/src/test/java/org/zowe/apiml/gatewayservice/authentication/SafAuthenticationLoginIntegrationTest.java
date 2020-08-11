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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.zowe.apiml.util.categories.MainframeDependentTests;

/**
 * Test that when valid credentials are provided the SAF authentication provider will accept them and the valid token
 * will be produced.
 *
 * Also verify that the invalid credentials will be properly rejected.
 */
@MainframeDependentTests
public class SafAuthenticationLoginIntegrationTest extends Login {
    @BeforeAll
    static void switchToSafProvider() {
        // Change the configuration via the configuration Management provided by the Spring
        System.out.println("Switch to SAF");

        // Store the default authentication provider and then go back to it.

    }

    @AfterAll
    static void switchToZosmfProvider() {
        System.out.println("Switch to zOSMF");
    }
}
