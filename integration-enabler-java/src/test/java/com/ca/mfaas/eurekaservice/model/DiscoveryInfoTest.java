/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DiscoveryInfoTest {
    @Test
    public void discoveryInfoTest() throws Exception {
        final String hostName = "hostName";
        final Boolean secure = true;
        final String serviceName = "serviceName";
        final Integer port = 8080;
        final String serviceType = "serviceType";
        final String serviceTitle = "serviceTitle";
        final Boolean enableApiDoc = true;
        final String description = "description";

        DiscoveryInfo discoveryInfo = new DiscoveryInfo(hostName, secure, serviceName, port, serviceType, serviceTitle, enableApiDoc, description);
        assertEquals(discoveryInfo.getHostName(), hostName);
        assertEquals(discoveryInfo.getSecure(), secure);
        assertEquals(discoveryInfo.getServiceName(), serviceName);
        assertEquals(discoveryInfo.getPort(), port);
        assertEquals(discoveryInfo.getServiceType(), serviceType);
        assertEquals(discoveryInfo.getServiceTitle(), serviceTitle);
        assertEquals(discoveryInfo.getEnableApiDoc(), enableApiDoc);
        assertEquals(discoveryInfo.getDescription(), description);

    }

}
