/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.services.ServiceInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModulithConfigTest {

    private static final String CLIENT_SERVICE_ID = "discoverableclient";

    @Test
    void givenServiceExistInDiscoveryClient_thenReturnServiceInfo() {

        var discoveryClient = mock(DiscoveryClient.class);

        when(discoveryClient.getServices())
            .thenReturn(Arrays.asList(CLIENT_SERVICE_ID));
        var ii = new InstanceInfo(CLIENT_SERVICE_ID, null, null, "192.168.0.1", null, new InstanceInfo.PortWrapper(true, 9090),
            null, null, null, null, null, null, null, 0, null, "hostname", InstanceInfo.InstanceStatus.UP, null, null, null, null, null,
            null, null, null, null);
        var serviceInstance = new EurekaServiceInstance(ii);
        when(discoveryClient.getInstances(CLIENT_SERVICE_ID))
            .thenReturn(Arrays.asList(serviceInstance));
        ModulithConfig mc = new ModulithConfig(null, null, null, null, null, null);
        var eurekaParser = mock(EurekaMetadataParser.class);
        when(eurekaParser.parseAuthentication(any())).thenReturn(new Authentication(AuthenticationScheme.ZOWE_JWT, "appl"));
        var basicInfoService = mc.basicInfoService(discoveryClient, eurekaParser);
        List<ServiceInfo> servicesInfo = basicInfoService.getServicesInfo();


        assertEquals(1, servicesInfo.size());
        assertThat(servicesInfo, contains(
            hasProperty("serviceId", is(CLIENT_SERVICE_ID))
        ));
    }

    @Test
    void givenNoServiceFoundInDiscoveryClient_thenReturnEmptyList() {

        var discoveryClient = mock(DiscoveryClient.class);

        when(discoveryClient.getServices())
            .thenReturn(Collections.emptyList());

        ModulithConfig mc = new ModulithConfig(null, null, null, null, null, null);
        var eurekaParser = mock(EurekaMetadataParser.class);
        var basicInfoService = mc.basicInfoService(discoveryClient, eurekaParser);
        List<ServiceInfo> servicesInfo = basicInfoService.getServicesInfo();
        assertThat(servicesInfo, emptyIterable());
    }

}
