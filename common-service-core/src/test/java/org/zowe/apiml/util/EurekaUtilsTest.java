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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.REGISTRATION_TYPE;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.RegistrationType.ADDITIONAL;
import static org.zowe.apiml.product.constants.CoreService.GATEWAY;

class EurekaUtilsTest {

    @Test
    void test() {
        assertEquals("abc", EurekaUtils.getServiceIdFromInstanceId("123:abc:def:::::xyz"));
        assertEquals("abc", EurekaUtils.getServiceIdFromInstanceId("123:abc:def"));
        assertEquals("", EurekaUtils.getServiceIdFromInstanceId("123::def"));
        assertEquals("", EurekaUtils.getServiceIdFromInstanceId("::"));
        assertNull(EurekaUtils.getServiceIdFromInstanceId(":"));
        assertNull(EurekaUtils.getServiceIdFromInstanceId(""));
        assertNull(EurekaUtils.getServiceIdFromInstanceId("abc"));
    }

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

        assertEquals("http://hostname1:80", EurekaUtils.getUrl(ii1));
        assertEquals("https://locahost:443", EurekaUtils.getUrl(ii2));
    }

    @Nested
    class PrimaryAndSecondaryRegistration {

        private static final String PRIMARY = "primary";
        private static final String SECONDARY = "secondary";

        private DiscoveryClient discoveryClient;

        @BeforeEach
        void init() {
            discoveryClient = mock(DiscoveryClient.class);

            InstanceInfo instanceInfoPrimary = InstanceInfo.Builder.newBuilder()
                .setAppName(PRIMARY)
                .setInstanceId(String.format("x:%s:1", PRIMARY))
                .build();
            ServiceInstance serviceInstancePrimary = new EurekaServiceInstance(instanceInfoPrimary);
            doReturn(Collections.singletonList(serviceInstancePrimary)).when(discoveryClient).getInstances(PRIMARY);

            InstanceInfo instanceInfoSecondary = InstanceInfo.Builder.newBuilder()
                .setAppName(GATEWAY.getServiceId())
                .setInstanceId(String.format("x:%s:1", GATEWAY.getServiceId()))
                .setMetadata(Map.of(
                    APIML_ID, SECONDARY,
                    REGISTRATION_TYPE, ADDITIONAL.getValue()
                ))
                .build();
            ServiceInstance serviceInstanceSecondary = new EurekaServiceInstance(instanceInfoSecondary);
            doReturn(Collections.singletonList(serviceInstanceSecondary)).when(discoveryClient).getInstances(GATEWAY.getServiceId());
        }

        @Test
        void givenPrimaryRegistration_whenGetInstanceInfo_thenReturnInstanceInfo() {
            var instance = EurekaUtils.getInstanceInfo(discoveryClient, PRIMARY);
            assertTrue(instance.isPresent());
            assertEquals(PRIMARY, instance.get().getServiceId().toLowerCase());
        }

        @Test
        void givenSecondaryRegistration_whenGetInstanceInfo_thenReturnInstanceInfo() {
            var instance = EurekaUtils.getInstanceInfo(discoveryClient, SECONDARY);
            assertTrue(instance.isPresent());
            assertEquals(GATEWAY.getServiceId(), instance.get().getServiceId().toLowerCase());
        }

        @Test
        void givenUnknownServiceId_whenGetInstanceInfo_thenReturnEmptyOptional() {
            var instance = EurekaUtils.getInstanceInfo(discoveryClient, "unknown");
            assertTrue(instance.isEmpty());
        }

    }

}
