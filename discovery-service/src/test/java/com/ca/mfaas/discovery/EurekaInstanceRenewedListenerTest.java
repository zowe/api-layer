package com.ca.mfaas.discovery;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import com.netflix.appinfo.InstanceInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRenewedEvent;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EurekaInstanceRenewedListenerTest {

    @InjectMocks
    private EurekaInstanceRenewedListener listener;

    @Mock
    private GatewayNotifier notifier;

    private EurekaInstanceRenewedEvent createEvent(String instanceId) {
        InstanceInfo instanceInfo = mock(InstanceInfo.class);
        when(instanceInfo.getInstanceId()).thenReturn(instanceId);

        EurekaInstanceRenewedEvent out = mock(EurekaInstanceRenewedEvent.class);
        when(out.getInstanceInfo()).thenReturn(instanceInfo);

        return out;
    }

    @Test
    public void testListen() {
        listener.listen(createEvent("host:service:instance"));
        verify(notifier, times(1)).serviceUpdated("service");
        listener.listen(createEvent("unknown format"));
        verify(notifier, times(1)).serviceUpdated(null);
    }

}
