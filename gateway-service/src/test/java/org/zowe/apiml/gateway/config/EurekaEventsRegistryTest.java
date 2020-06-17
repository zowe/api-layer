/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import com.netflix.discovery.EurekaClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.zowe.apiml.gateway.metadata.service.RibbonMetadataProcessor;

import java.util.Collections;

import static org.mockito.Mockito.*;

class EurekaEventsRegistryTest {
    @Test
    void givenApplicationReadyEvent_whenEurekaRegisterEvent_thenCallRegisterEventListener() {
        EurekaClient eurekaClient = mock(EurekaClient.class);
        EurekaEventsRegistry eventRegister = new EurekaEventsRegistry(eurekaClient,
            Collections.singletonList(mock(RibbonMetadataProcessor.class)));

        eventRegister.onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(eurekaClient, times(1)).registerEventListener(any());
    }
}
