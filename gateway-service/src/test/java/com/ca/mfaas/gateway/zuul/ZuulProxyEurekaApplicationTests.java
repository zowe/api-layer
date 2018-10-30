/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.zuul;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
@Ignore
public class ZuulProxyEurekaApplicationTests {

    @Autowired
    DiscoveryClient discoveryClient;

    @Test
    public void contextLoads() {
    }

    @Test
    public void discoveryClientIsEureka() {
        assertTrue("discoveryClient is wrong type", discoveryClient instanceof CompositeDiscoveryClient);
        assertTrue("composite discovery client should compose Eureka Discovery Client with highest precedence",
            ((CompositeDiscoveryClient) discoveryClient).getDiscoveryClients().get(0) instanceof EurekaDiscoveryClient);
    }

}
