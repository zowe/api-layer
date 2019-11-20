/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.hellospring.listener;

import org.junit.Test;


public class ApiDiscoveryListenerTest {

    @Test
    public void contextStartsAndStopsTest() {
        ApiDiscoveryListener contextListener = new ApiDiscoveryListener();
        // The parameter can't be null in reality.
        //  TODO: Rewrite the test with some context variations
            //contextListener.contextInitialized(null);
            //contextListener.contextDestroyed(null);
    }
}
