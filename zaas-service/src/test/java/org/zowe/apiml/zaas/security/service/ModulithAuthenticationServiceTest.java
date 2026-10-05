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

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModulithAuthenticationServiceTest {

    @Test
    void getInvalidateUrl() {
        ModulithAuthenticationService service =
            new ModulithAuthenticationService(null, null, null, null, null, null, null, null, null);
        var instance = new DefaultServiceInstance("localhost:gateway:443", "gateway", "localhost", 443, true);

        assertEquals(
            "https://localhost:443/gateway/api/v1/auth/invalidate",
            service.getInvalidateUrl(instance));
    }

}
