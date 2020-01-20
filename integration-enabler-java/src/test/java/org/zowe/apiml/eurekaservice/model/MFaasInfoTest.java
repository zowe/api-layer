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

public class MFaasInfoTest {
    @Test
    public void mfaasInfoTest() throws Exception {

        final DiscoveryInfo discoveryInfo = new DiscoveryInfo(
            "hostname",
            true, "serviceName",
            8080, "serviceType",
            "serviceTitle",
            true,
            "description");

        MFaasInfo mFaasInfo = new MFaasInfo(discoveryInfo);
        assertEquals(mFaasInfo.getDiscoveryInfo(), discoveryInfo);
    }

}
