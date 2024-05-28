/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.config;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.zaas.discovery.ApimlDiscoveryClient;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class DiscoveryClientWrapperTest {

    @Test
    void givenExistingListOfClient_thenCallShutdownForEach() {
        ApimlDiscoveryClient client1 = mock(ApimlDiscoveryClient.class);
        ApimlDiscoveryClient client2 = mock(ApimlDiscoveryClient.class);
        DiscoveryClientWrapper wrapper = new DiscoveryClientWrapper(Arrays.asList(client1, client2));
        wrapper.shutdown();
        verify(client1, times(1)).shutdown();
        verify(client2, times(1)).shutdown();
    }

    @Test
    void givenNullListOfClient_thenSkipShutdown() {
        DiscoveryClientWrapper wrapper = new DiscoveryClientWrapper(null);
        assertDoesNotThrow(wrapper::shutdown);
    }
}
