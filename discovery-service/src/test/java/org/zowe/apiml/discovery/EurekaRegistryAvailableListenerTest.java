/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.zowe.apiml.discovery.staticdef.StaticServicesRegistrationService;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class EurekaRegistryAvailableListenerTest {

    @Mock private StaticServicesRegistrationService registrationService;
    @Mock private ServiceStartupEventHandler startupEventHandler;

    private EurekaRegistryAvailableListener listener;

    @BeforeEach
    void setUp() {
        this.listener = new EurekaRegistryAvailableListener(registrationService, startupEventHandler);
    }

    @Test
    void onEvent_notifyStartup() {
        doNothing().when(registrationService).registerServices();
        doNothing().when(startupEventHandler).onServiceStartup(anyString(), anyInt());

        listener.onApplicationEvent(mock(EurekaRegistryAvailableEvent.class));

        verify(startupEventHandler, times(1)).onServiceStartup("Discovery Service", 5);
    }

    @Test
    void onMultipleEvents_notifyOnce() {
        doNothing().when(registrationService).registerServices();
        doNothing().when(startupEventHandler).onServiceStartup(anyString(), anyInt());

        listener.onApplicationEvent(mock(EurekaRegistryAvailableEvent.class));

        verify(startupEventHandler, times(1)).onServiceStartup("Discovery Service", 5);

        listener.onApplicationEvent(mock(EurekaRegistryAvailableEvent.class));

        verifyNoMoreInteractions(startupEventHandler);
    }

}
