/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EurekaUtilsTest {

    @Test
    void test() {
        assertEquals("abc", EurekaUtils.getServiceIdFromInstanceId("123:abc:def:::::xyz"));
        assertEquals("abc", EurekaUtils.getServiceIdFromInstanceId("123:abc:def"));
        assertEquals("", EurekaUtils.getServiceIdFromInstanceId("123::def"));
        assertEquals("", EurekaUtils.getServiceIdFromInstanceId("::"));
        assertNull(EurekaUtils.getServiceIdFromInstanceId(":"));
        assertNull(EurekaUtils.getServiceIdFromInstanceId(""));
        assertNull(EurekaUtils.getServiceIdFromInstanceId("abc"));
    }

    private InstanceInfo createInstanceInfo(String host, int port, int securePort, boolean isSecureEnabled) {
        InstanceInfo out = mock(InstanceInfo.class);
        when(out.getHostName()).thenReturn(host);
        when(out.getPort()).thenReturn(port);
        when(out.getSecurePort()).thenReturn(securePort);
        when(out.isPortEnabled(InstanceInfo.PortType.SECURE)).thenReturn(isSecureEnabled);
        return out;
    }

    @Test
    void testGetUrl() {
        InstanceInfo ii1 = createInstanceInfo("hostname1", 80, 0, false);
        InstanceInfo ii2 = createInstanceInfo("locahost", 80, 443, true);

        assertEquals("http://hostname1:80", EurekaUtils.getUrl(ii1));
        assertEquals("https://locahost:443", EurekaUtils.getUrl(ii2));
    }

}
