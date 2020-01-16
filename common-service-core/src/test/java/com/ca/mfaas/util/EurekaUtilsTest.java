package com.ca.mfaas.util;/*
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EurekaUtilsTest {

    @Test
    public void test() {
        assertEquals("abc", EurekaUtils.getServiceIdFromInstanceId("123:abc:def:::::xyz"));
        assertEquals("abc", EurekaUtils.getServiceIdFromInstanceId("123:abc:def"));
        assertEquals("", EurekaUtils.getServiceIdFromInstanceId("123::def"));
        assertEquals("", EurekaUtils.getServiceIdFromInstanceId("::"));
        assertNull(EurekaUtils.getServiceIdFromInstanceId(":"));
        assertNull(EurekaUtils.getServiceIdFromInstanceId(""));
        assertNull(EurekaUtils.getServiceIdFromInstanceId("abc"));
    }

    private InstanceInfo createInstanceInfo(String ip, int port, int securePort) {
        InstanceInfo out = mock(InstanceInfo.class);
        when(out.getIPAddr()).thenReturn(ip);
        when(out.getPort()).thenReturn(port);
        when(out.getSecurePort()).thenReturn(securePort);
        return out;
    }

    @Test
    public void testGetUrl() {
        InstanceInfo ii1 = createInstanceInfo("127.0.0.1", 80, 0);
        InstanceInfo ii2 = createInstanceInfo("192.168.0.1", 80, 443);

        assertEquals("http://127.0.0.1:80", EurekaUtils.getUrl(ii1));
        assertEquals("https://192.168.0.1:443", EurekaUtils.getUrl(ii2));
    }

}
