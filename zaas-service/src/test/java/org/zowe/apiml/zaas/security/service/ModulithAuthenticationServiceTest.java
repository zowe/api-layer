/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModulithAuthenticationServiceTest {
    @Test
    void getInvalidateUrl() {
        ModulithAuthenticationService service = new ModulithAuthenticationService(null, null, null, null, null, null, null, null);
        InstanceInfo instanceInfo = mock(InstanceInfo.class);
        when(instanceInfo.getHostName()).thenReturn("localhost");
        when(instanceInfo.getSecurePort()).thenReturn(443);
        when(instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE)).thenReturn(true);
        String invalidateUrl = service.getInvalidateUrl(instanceInfo);
        assertEquals("https://localhost:443/gateway/api/v1/auth/invalidate/", invalidateUrl);
    }
}
