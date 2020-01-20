/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.eurekaservice.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InstanceInfoTest {

    @Test
    public void instanceInfoTest() throws Exception {
        final App app = new App("name", "description", "1.0");


        final DiscoveryInfo discoveryInfo = new DiscoveryInfo(
            "hostname",
            true, "serviceName",
            8080, "serviceType",
            "serviceTitle",
            true,
            "description");

        final MFaasInfo mfaasInfo = new MFaasInfo(discoveryInfo);

        InstanceInfo instanceInfo = new InstanceInfo(app, mfaasInfo);
        assertEquals(instanceInfo.getApp(), app);
        assertEquals(instanceInfo.getMFaaSInfo(), mfaasInfo);
    }

}
