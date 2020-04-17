/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.startup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.startup.impl.ApiMediationLayerStartupChecker;
import org.zowe.apiml.util.categories.StartupCheck;

import static junit.framework.TestCase.assertTrue;

@StartupCheck
public class ApiMediationLayerStartTest {

    @BeforeEach
    public void setUp() {
        new ApiMediationLayerStartupChecker().waitUntilReady();
    }

    @Test
    public void checkApiMediationLayerStart() {
        assertTrue(true);
    }
}
