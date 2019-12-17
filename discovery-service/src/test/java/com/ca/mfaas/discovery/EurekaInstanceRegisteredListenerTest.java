/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discovery;

import com.ca.mfaas.discovery.metadata.MetadataDefaultsService;
import com.ca.mfaas.discovery.metadata.MetadataTranslationService;
import com.netflix.appinfo.InstanceInfo;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class EurekaInstanceRegisteredListenerTest {

    @Test
    public void getServiceId() {

        MetadataTranslationService metadataTranslationService = Mockito.mock(MetadataTranslationService.class);
        MetadataDefaultsService metadataDefaultsService = Mockito.mock(MetadataDefaultsService.class);

        EurekaInstanceRegisteredListener eirl = new EurekaInstanceRegisteredListener(metadataTranslationService, metadataDefaultsService);

        assertEquals("abc", eirl.getServiceId("123:abc:def:::::xyz"));
        assertEquals("abc", eirl.getServiceId("123:abc:def"));
        assertEquals("", eirl.getServiceId("123::def"));
        assertEquals("", eirl.getServiceId("::"));
        assertNull(eirl.getServiceId(":"));
        assertNull(eirl.getServiceId(""));
        assertNull(eirl.getServiceId("abc"));

        doAnswer(
            x -> {
                assertEquals("serviceName", x.getArgument(0));
                return null;
            }
        ).when(metadataDefaultsService).updateMetadata(anyString(), any());

        InstanceInfo instanceInfo = mock(InstanceInfo.class);
        when(instanceInfo.getId()).thenReturn("1:serviceName:2");
        EurekaInstanceRegisteredEvent event = mock(EurekaInstanceRegisteredEvent.class);
        when(event.getInstanceInfo()).thenReturn(instanceInfo);

        eirl.listen(event);

        verify(metadataDefaultsService, times(1)).updateMetadata(anyString(), any());
    }

}
