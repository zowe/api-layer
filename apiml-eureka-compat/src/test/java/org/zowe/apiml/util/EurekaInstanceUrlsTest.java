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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Moved here with {@link EurekaInstanceUrls#getUrl} when it came out of {@code EurekaUtils}, so that
 * common-service-core no longer needs a Eureka dependency. The cases are unchanged.
 */
class EurekaInstanceUrlsTest {

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

        assertEquals("http://hostname1:80", EurekaInstanceUrls.getUrl(ii1));
        assertEquals("https://locahost:443", EurekaInstanceUrls.getUrl(ii2));
    }

}
