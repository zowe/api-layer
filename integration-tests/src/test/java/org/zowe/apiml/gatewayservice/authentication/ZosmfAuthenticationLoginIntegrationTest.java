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

import org.junit.jupiter.api.BeforeAll;
import org.zowe.apiml.util.categories.MainframeDependentTests;

@MainframeDependentTests
public class ZosmfAuthenticationLoginIntegrationTest extends Login {
    @BeforeAll
    static void switchToTestedProvider() {
        currentProvider = loadCurrentProvider();
        switchProvider("zosmf");
    }
}
