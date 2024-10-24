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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.zowe.apiml.discovery.metadata.MetadataDefaultsService;
import org.zowe.apiml.discovery.metadata.MetadataTranslationService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EurekaInstanceRegisteredListenerTest {

    @Mock
    private MetadataTranslationService metadataTranslationService;

    @Mock
    private MetadataDefaultsService metadataDefaultsService;

    private EurekaInstanceRegisteredListener eurekaInstanceRegisteredListener;

    private final Map<String, String> metadata = new HashMap<>();

    @BeforeEach
    void setUp() {
        eurekaInstanceRegisteredListener = new EurekaInstanceRegisteredListener(metadataTranslationService, metadataDefaultsService);
        reset(metadataTranslationService, metadataDefaultsService);
    }

    @Test
    void getServiceId() {
        var serviceId = "serviceName";

        doAnswer(
            x -> {
                assertEquals(serviceId, x.getArgument(0));
                return null;
            }
        ).when(metadataDefaultsService).updateMetadata(anyString(), any());

        InstanceInfo instanceInfo = mock(InstanceInfo.class);
        when(instanceInfo.getInstanceId()).thenReturn("1:" + serviceId + ":2");
        EurekaInstanceRegisteredEvent event = mock(EurekaInstanceRegisteredEvent.class);
        when(event.getInstanceInfo()).thenReturn(instanceInfo);

        eurekaInstanceRegisteredListener.listen(event);

        verifyListenerMetadataOperations(serviceId);
    }

    private EurekaInstanceRegisteredEvent createEvent(String instanceId) {
        InstanceInfo instanceInfo = mock(InstanceInfo.class);
        when(instanceInfo.getInstanceId()).thenReturn(instanceId);

        EurekaInstanceRegisteredEvent out = mock(EurekaInstanceRegisteredEvent.class);
        when(out.getInstanceInfo()).thenReturn(instanceInfo);

        return out;
    }

    @Test
    void registeredInstanceListen() {

        assertDoesNotThrow(() -> eurekaInstanceRegisteredListener.listen(createEvent("host:service:instance")));
        verifyListenerMetadataOperations("service");

        assertDoesNotThrow(() -> eurekaInstanceRegisteredListener.listen(createEvent("unknown format")));
        verifyListenerMetadataOperations(null);

        assertDoesNotThrow(() -> eurekaInstanceRegisteredListener.listen(createEvent("host:GATEWAY:instance")));
        verifyListenerMetadataOperations("GATEWAY");

    }

    private void verifyListenerMetadataOperations(String serviceId) {
        verify(metadataTranslationService, times(1)).translateMetadata(serviceId, metadata);
        verify(metadataDefaultsService, times(1)).updateMetadata(serviceId, metadata);
    }

}
