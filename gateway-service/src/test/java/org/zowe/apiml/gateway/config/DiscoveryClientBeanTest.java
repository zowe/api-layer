/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import com.netflix.appinfo.*;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscoveryClientBeanTest {
    DiscoveryClientConfig dcConfig;

    @BeforeEach
    void setup() {
        ApplicationContext context = mock(ApplicationContext.class);
        EurekaJerseyClientImpl.EurekaJerseyClientBuilder builder = mock(EurekaJerseyClientImpl.EurekaJerseyClientBuilder.class);
        dcConfig = new DiscoveryClientConfig(context, null, builder);
    }

    @Test
    void givenListOfCentralRegistryURLs_thenCreateNewDiscoveryClientForEach() {
        String[] centralRegistryUrls = {"https://localhost:10021/eureka", "https://localhost:10011/eureka"};
        ReflectionTestUtils.setField(dcConfig, "centralRegistryUrls", centralRegistryUrls);
        ApplicationInfoManager manager = mock(ApplicationInfoManager.class);
        InstanceInfo info = mock(InstanceInfo.class);
        when(manager.getInfo()).thenReturn(info);
        when(info.getIPAddr()).thenReturn("127.0.0.1");
        when(info.getDataCenterInfo()).thenReturn(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn));
        LeaseInfo leaseInfo = mock(LeaseInfo.class);
        when(info.getLeaseInfo()).thenReturn(leaseInfo);
        EurekaClientConfigBean bean = new EurekaClientConfigBean();
        DiscoveryClientWrapper wrapper = dcConfig.additionalDiscoverClientWrapper(manager, bean, null);
        assertEquals(2, wrapper.getDiscoveryClients().size());
    }

    @Test
    void discoveryClient() {

    }
}
