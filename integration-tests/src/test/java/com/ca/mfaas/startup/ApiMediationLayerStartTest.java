/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.startup;

import com.ca.mfaas.startup.impl.ApiMediationLayerStartupChecker;
import com.ca.mfaas.util.categories.StartupCheck;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static junit.framework.TestCase.assertTrue;

@Category(StartupCheck.class)
public class ApiMediationLayerStartTest {

    @Before
    public void setUp() {
        new ApiMediationLayerStartupChecker().waitUntilReady();
    }

    @Test
    public void checkApiMediationLayerStart() {
        assertTrue(true);
    }
}
