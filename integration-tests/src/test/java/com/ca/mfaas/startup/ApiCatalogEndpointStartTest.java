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

import com.ca.mfaas.startup.impl.ApiDocFormatStartupChecker;
import com.ca.mfaas.utils.categories.StartupCheck;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertTrue;

@Category(StartupCheck.class)
public class ApiCatalogEndpointStartTest {

    @Before
    public void setUpClass() {
        new ApiDocFormatStartupChecker().waitUntilReady();
    }

    @Test
    public void checkApiCatalogEndpointStart() {
        assertTrue(true);
    }
}
