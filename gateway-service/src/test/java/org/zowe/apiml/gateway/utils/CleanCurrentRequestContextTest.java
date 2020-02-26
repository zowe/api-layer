/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Makes sure that only one test class derived from this class is using
 * RequestContext.getCurrentContext at a time.
 */
public abstract class CleanCurrentRequestContextTest extends CurrentRequestContextTest {
    @BeforeEach
    public void setup() {
        this.lockAndClearRequestContext();
    }

    @AfterEach
    public void tearDown() {
        this.unlockRequestContext();
    }
}
