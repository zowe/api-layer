/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.utils;

import org.junit.After;
import org.junit.Before;

/**
 * Makes sure that only one test class derived from this class is using
 * RequestContext.getCurrentContext at a time.
 */
public abstract class CleanCurrentRequestContextTest extends CurrentRequestContextTest {
    @Before
    public void setup() {
        this.lockAndClearRequestContext();
    }

    @After
    public void tearDown() {
        this.unlockRequestContext();
    }
}
