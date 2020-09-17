package org.zowe.apiml.discovery;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import org.junit.Test;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;

import static org.mockito.Mockito.*;

public class EurekaInstanceCanceledListenerTest {

    private EurekaInstanceCanceledEvent createEvent(String serverId) {
        EurekaInstanceCanceledEvent out = mock(EurekaInstanceCanceledEvent.class);
        when(out.getServerId()).thenReturn(serverId);

        return out;
    }

    @Test
    public void testListen() {
        GatewayNotifier notifier = mock(GatewayNotifier.class);
        EurekaInstanceCanceledListener listener = new EurekaInstanceCanceledListener(notifier);

        listener.listen(createEvent("host:service:instance"));
        verify(notifier, times(1)).serviceCancelledRegistration("service");
        listener.listen(createEvent("unknown format"));
        verify(notifier, times(1)).serviceCancelledRegistration(null);
    }

}
