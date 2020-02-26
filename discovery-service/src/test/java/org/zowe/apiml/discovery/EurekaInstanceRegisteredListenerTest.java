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

import com.netflix.appinfo.InstanceInfo;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.zowe.apiml.discovery.metadata.MetadataDefaultsService;
import org.zowe.apiml.discovery.metadata.MetadataTranslationService;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class EurekaInstanceRegisteredListenerTest {

    @Test
    public void getServiceId() {
        MetadataTranslationService metadataTranslationService = Mockito.mock(MetadataTranslationService.class);
        MetadataDefaultsService metadataDefaultsService = Mockito.mock(MetadataDefaultsService.class);

        EurekaInstanceRegisteredListener eirl = new EurekaInstanceRegisteredListener(metadataTranslationService, metadataDefaultsService, mock(GatewayNotifier.class));

        doAnswer(
            x -> {
                assertEquals("serviceName", x.getArgument(0));
                return null;
            }
        ).when(metadataDefaultsService).updateMetadata(anyString(), any());

        InstanceInfo instanceInfo = mock(InstanceInfo.class);
        when(instanceInfo.getInstanceId()).thenReturn("1:serviceName:2");
        EurekaInstanceRegisteredEvent event = mock(EurekaInstanceRegisteredEvent.class);
        when(event.getInstanceInfo()).thenReturn(instanceInfo);

        eirl.listen(event);

        verify(metadataDefaultsService, times(1)).updateMetadata(anyString(), any());
    }

    private EurekaInstanceRegisteredEvent createEvent(String instanceId) {
        InstanceInfo instanceInfo = mock(InstanceInfo.class);
        when(instanceInfo.getInstanceId()).thenReturn(instanceId);

        EurekaInstanceRegisteredEvent out = mock(EurekaInstanceRegisteredEvent.class);
        when(out.getInstanceInfo()).thenReturn(instanceInfo);

        return out;
    }

    @Test
    public void testListen() {
        GatewayNotifier notifier = mock(GatewayNotifier.class);
        EurekaInstanceRegisteredListener listener = new EurekaInstanceRegisteredListener(Mockito.mock(MetadataTranslationService.class), Mockito.mock(MetadataDefaultsService.class), notifier);

        listener.listen(createEvent("host:service:instance"));
        verify(notifier, times(1)).serviceUpdated("service", "host:service:instance");
        listener.listen(createEvent("unknown format"));
        verify(notifier, times(1)).serviceUpdated(null, "unknown format");
    }

}
