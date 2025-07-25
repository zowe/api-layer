/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CachingServiceStartupListenerTest {

    @Mock private ServiceStartupEventHandler startupEventHandler;

    private CachingServiceStartupListener listener;

    @BeforeEach
    void setUp() {
        listener = new CachingServiceStartupListener(startupEventHandler);
    }

    @Test
    void onEvent_thenNotifyStartup() {
        doNothing().when(startupEventHandler).onServiceStartup(anyString(), anyInt());

        listener.onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(startupEventHandler, times(1)).onServiceStartup("Caching Service", 5);
    }

}
