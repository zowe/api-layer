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

import org.junit.jupiter.api.Test;
import org.springframework.cloud.commons.util.InetUtils;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.services.ServiceInfo;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ModulithConfigTest {

    private static final String CLIENT_SERVICE_ID = "discoverableclient";

    @Test
    void givenServiceExistInDiscoveryClient_thenReturnServiceInfo() {

        var registry = mock(ServiceRegistry.class);

        when(registry.serviceIds()).thenReturn(List.of(CLIENT_SERVICE_ID));
        var serviceInstance = ServiceInstance.builder()
            .instanceId("hostname:" + CLIENT_SERVICE_ID + ":9090")
            .appName(CLIENT_SERVICE_ID)
            .hostName("hostname")
            .ipAddr("192.168.0.1")
            .port(9090, true)
            .status(InstanceStatus.UP)
            .build();
        when(registry.instances(CLIENT_SERVICE_ID)).thenReturn(List.of(serviceInstance));
        ModulithConfig mc = new ModulithConfig(null, null, null, null, null, null, null);
        var eurekaParser = mock(EurekaMetadataParser.class);
        when(eurekaParser.parseAuthentication(any())).thenReturn(new Authentication(AuthenticationScheme.ZOWE_JWT, "appl"));
        var basicInfoService = mc.basicInfoService(registry, eurekaParser);
        List<ServiceInfo> servicesInfo = basicInfoService.getServicesInfo();


        assertEquals(1, servicesInfo.size());
        assertThat(servicesInfo, contains(
            hasProperty("serviceId", is(CLIENT_SERVICE_ID))
        ));
    }

    @Test
    void givenNoServiceFoundInDiscoveryClient_thenReturnEmptyList() {

        var registry = mock(ServiceRegistry.class);

        when(registry.serviceIds()).thenReturn(Collections.emptyList());

        ModulithConfig mc = new ModulithConfig(null, null, null, null, null, null, null);
        var eurekaParser = mock(EurekaMetadataParser.class);
        var basicInfoService = mc.basicInfoService(registry, eurekaParser);
        List<ServiceInfo> servicesInfo = basicInfoService.getServicesInfo();
        assertThat(servicesInfo, emptyIterable());
    }

    @Test
    void givenIpAddressUnset_whenValidateIpAddress_thenResolveItFromInetUtils() throws Exception {
        var resolvedIp = "1.2.3.4";
        var inetAddressMock = mock(InetAddress.class);
        var inetUtilsMock = mock(InetUtils.class);

        when(inetAddressMock.getHostAddress()).thenReturn(resolvedIp);
        when(inetUtilsMock.findFirstNonLoopbackAddress()).thenReturn(inetAddressMock);

        var mc = new ModulithConfig(null, null, null, null, null, null, inetUtilsMock);
        mc.validateIpAddress();

        assertEquals(resolvedIp, getIpAddress(mc));
    }

    @Test
    void givenIpAddressAlreadySet_whenValidateIpAddress_thenKeepIt() throws Exception {
        var presetIp = "5.6.7.8";
        var inetUtilsMock = mock(InetUtils.class);

        var mc = new ModulithConfig(null, null, null, null, null, null, inetUtilsMock);
        setIpAddress(mc, presetIp);
        mc.validateIpAddress();

        assertEquals(presetIp, getIpAddress(mc));
        verifyNoInteractions(inetUtilsMock);
    }

    private static String getIpAddress(ModulithConfig mc) throws Exception {
        var field = ModulithConfig.class.getDeclaredField("ipAddress");
        field.setAccessible(true);
        return (String) field.get(mc);
    }

    private static void setIpAddress(ModulithConfig mc, String value) throws Exception {
        var field = ModulithConfig.class.getDeclaredField("ipAddress");
        field.setAccessible(true);
        field.set(mc, value);
    }

}
