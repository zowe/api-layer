/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.services.status.listeners;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.apicatalog.instance.InstanceInitializeService;

import static org.mockito.Mockito.*;

class GatewayLookupEventListenerTest {

    @Test
    void retrieveAndRegisterAllInstancesWithCatalog_onlyOnce() throws Exception {
        InstanceInitializeService instanceInitializeService = mock(InstanceInitializeService.class);
        GatewayLookupEventListener gatewayLookupEventListener = new GatewayLookupEventListener(instanceInitializeService);
        for (int i = 0; i < 10; i++) {
            gatewayLookupEventListener.onApplicationEvent();
        }
        verify(instanceInitializeService, times(1)).retrieveAndRegisterAllInstancesWithCatalog();
    }


}
