/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.gateway;

import com.google.common.cache.CacheBuilder;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.product.constants.CoreService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdditionalRegistrationGatewayRegistryTest {

    private DiscoveryClient discoveryClientMock;
    private AdditionalRegistrationGatewayRegistry gatewayRegistry;

    // Cloud Gateway
    private final String CGW_INSTANCE_ID = "cgw-instance-id";
    private final String CGW_IP_ADDRESS = "1.1.1.1";
    private final String CGW_HOSTNAME = "cgw-hostname";

    private final Application CGW_APPLICATION =
        new Application(CoreService.GATEWAY.getServiceId(),
            Arrays.asList(InstanceInfo.Builder.newBuilder()
                .setAppName(CoreService.GATEWAY.getServiceId())
                .setInstanceId(CGW_INSTANCE_ID)
                .setHostName(CGW_HOSTNAME)
                .setIPAddr(CGW_IP_ADDRESS)
                .build()));

    // Gateway with single IP address
    private final String GW1_INSTANCE_ID = "gw1-instance-id";
    private final String GW1_IP_ADDRESS = "2.2.2.2";
    private final String GW1_HOSTNAME = "gw1-hostname";

    private final InstanceInfo GW1_INSTANCE_INFO = InstanceInfo.Builder.newBuilder()
        .setAppName(CoreService.CLOUD_GATEWAY.getServiceId())
        .setInstanceId(GW1_INSTANCE_ID)
        .setHostName(GW1_HOSTNAME)
        .setIPAddr(GW1_IP_ADDRESS)
        .build();

    private final Application GW1_APPLICATION =
        new Application(CoreService.CLOUD_GATEWAY.getServiceId(),
            Arrays.asList(GW1_INSTANCE_INFO));

    // Gateway resolved to multiple ip addresses
    private final String GW2_INSTANCE_ID = "gw2-instance-id";
    private final String GW2_IP_ADDRESS = "3.3.3.3";
    private final String GW2_IP_ADDRESS_FROM_DNS = "4.4.4.4";
    private final String GW2_HOSTNAME = "gw2-hostname";

    private final InstanceInfo GW2_INSTANCE_INFO =
        InstanceInfo.Builder.newBuilder()
            .setAppName(CoreService.CLOUD_GATEWAY.getServiceId())
            .setInstanceId(GW2_INSTANCE_ID)
            .setHostName(GW2_HOSTNAME)
            .setIPAddr(GW2_IP_ADDRESS)
            .build();

    private final Application GW2_APPLICATION =
        new Application(CoreService.CLOUD_GATEWAY.getServiceId(),
            Arrays.asList(GW2_INSTANCE_INFO));

    // Appplication with both gateways
    private Application ALL_GW_APPLICATION =
        new Application(CoreService.CLOUD_GATEWAY.getServiceId(),
            Arrays.asList(GW1_INSTANCE_INFO, GW2_INSTANCE_INFO));

    @BeforeEach
    @SneakyThrows
    void setup() {
        discoveryClientMock = Mockito.mock(DiscoveryClient.class);
        gatewayRegistry = new AdditionalRegistrationGatewayRegistry();
        gatewayRegistry.registryExpiration = Duration.ofSeconds(300);
        gatewayRegistry.init();
    }

    @Test
    void eventListenerRegistered() {
        gatewayRegistry.registerAdditionalRegistrationsGatewayRegistryRefresh(discoveryClientMock);

        verify(discoveryClientMock, times(1))
            .registerEventListener(any());
    }

    @Test
    void whenClouGateway_thenAddIpToRegistry() {
        when(discoveryClientMock.getApplication(CoreService.CLOUD_GATEWAY.getServiceId())).thenReturn(CGW_APPLICATION);
        when(discoveryClientMock.getApplication(CoreService.GATEWAY.getServiceId())).thenReturn(null);

        gatewayRegistry.cacheRefreshEventHandler(new CacheRefreshedEvent(), discoveryClientMock);

        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().size(), is(1));
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(CGW_INSTANCE_ID), is(Arrays.asList(CGW_IP_ADDRESS)));

        assertThat(gatewayRegistry.additionalGatewayIpAddressesReference.get().size(), is(1));
        assertTrue(gatewayRegistry.additionalGatewayIpAddressesReference.get().contains(CGW_IP_ADDRESS));
    }

    @Test
    void whenClouGateway_andGateway_thenAddIpToRegistry() {
        when(discoveryClientMock.getApplication(CoreService.CLOUD_GATEWAY.getServiceId())).thenReturn(CGW_APPLICATION);
        when(discoveryClientMock.getApplication(CoreService.GATEWAY.getServiceId())).thenReturn(GW1_APPLICATION);

        gatewayRegistry.cacheRefreshEventHandler(new CacheRefreshedEvent(), discoveryClientMock);

        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().size(), is(2));
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(CGW_INSTANCE_ID), is(Arrays.asList(CGW_IP_ADDRESS)));
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(GW1_INSTANCE_ID), is(Arrays.asList(GW1_IP_ADDRESS)));

        assertThat(gatewayRegistry.additionalGatewayIpAddressesReference.get().size(), is(2));
        assertTrue(gatewayRegistry.additionalGatewayIpAddressesReference.get().containsAll(Arrays.asList(CGW_IP_ADDRESS, GW1_IP_ADDRESS)));
    }

    @Test
    void whenClouGateway_andGatewayWithDNSResolution_thenAddIpToRegistry() throws UnknownHostException {
        when(discoveryClientMock.getApplication(CoreService.CLOUD_GATEWAY.getServiceId())).thenReturn(CGW_APPLICATION);
        when(discoveryClientMock.getApplication(CoreService.GATEWAY.getServiceId())).thenReturn(GW2_APPLICATION);


        InetAddress[] resolvedCgwHostname = new InetAddress[]{InetAddress.getByName(CGW_IP_ADDRESS)};
        InetAddress[] resolvedCgwIpAddress = new InetAddress[]{InetAddress.getByName(CGW_IP_ADDRESS)};
        InetAddress[] resolvedGw2Hostname = new InetAddress[]{InetAddress.getByName(GW2_IP_ADDRESS_FROM_DNS)};
        InetAddress[] resolvedGw2IpAddress = new InetAddress[]{InetAddress.getByName(GW2_IP_ADDRESS)};

        try (MockedStatic<InetAddress> inetAddressMocked = Mockito.mockStatic(InetAddress.class)) {
            // We cannot use .callReaLMethod() as it relies on native call that cannot be used via mock
            inetAddressMocked.when(() -> InetAddress.getAllByName(CGW_HOSTNAME)).thenReturn(resolvedCgwHostname);
            inetAddressMocked.when(() -> InetAddress.getAllByName(CGW_IP_ADDRESS)).thenReturn(resolvedCgwIpAddress);
            inetAddressMocked.when(() -> InetAddress.getAllByName(GW2_IP_ADDRESS)).thenReturn(resolvedGw2IpAddress);
            inetAddressMocked.when(() -> InetAddress.getAllByName(GW2_HOSTNAME)).thenReturn(resolvedGw2Hostname);

            gatewayRegistry.cacheRefreshEventHandler(new CacheRefreshedEvent(), discoveryClientMock);
        }

        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().size(), is(2));
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(CGW_INSTANCE_ID), is(Arrays.asList(CGW_IP_ADDRESS)));
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(GW2_INSTANCE_ID).size(), is(2));
        assertTrue(gatewayRegistry.knownAdditionalGateways.asMap().get(GW2_INSTANCE_ID).containsAll(Arrays.asList(GW2_IP_ADDRESS, GW2_IP_ADDRESS_FROM_DNS)));

        assertThat(gatewayRegistry.additionalGatewayIpAddressesReference.get().size(), is(3));
        assertTrue(gatewayRegistry.additionalGatewayIpAddressesReference.get().containsAll(Arrays.asList(CGW_IP_ADDRESS, GW2_IP_ADDRESS, GW2_IP_ADDRESS_FROM_DNS)));
    }

    @Test
    void whenClouGateway_andMultipleGateways_thenAddIpToRegistry() {
        when(discoveryClientMock.getApplication(CoreService.CLOUD_GATEWAY.getServiceId())).thenReturn(CGW_APPLICATION);
        when(discoveryClientMock.getApplication(CoreService.GATEWAY.getServiceId())).thenReturn(ALL_GW_APPLICATION);

        gatewayRegistry.cacheRefreshEventHandler(new CacheRefreshedEvent(), discoveryClientMock);

        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().size(), is(3));
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(CGW_INSTANCE_ID), is(Arrays.asList(CGW_IP_ADDRESS)));
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(GW1_INSTANCE_ID), is(Arrays.asList(GW1_IP_ADDRESS)));
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(GW2_INSTANCE_ID), is(Arrays.asList(GW2_IP_ADDRESS)));

        assertThat(gatewayRegistry.additionalGatewayIpAddressesReference.get().size(), is(3));
        assertTrue(gatewayRegistry.additionalGatewayIpAddressesReference.get().containsAll(Arrays.asList(CGW_IP_ADDRESS, GW1_IP_ADDRESS, GW2_IP_ADDRESS)));
    }

    @Test
    void clearGwRegistryCache() {
        gatewayRegistry.knownAdditionalGateways = CacheBuilder.newBuilder().expireAfterWrite(1L, SECONDS).build();
        whenClouGateway_thenAddIpToRegistry();
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(CGW_INSTANCE_ID).size(), is(1));

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
            // We cannot validate on size as the entries may still be in the cache but masked
            assertNull(gatewayRegistry.knownAdditionalGateways.asMap().get(CGW_INSTANCE_ID))
        );

        when(discoveryClientMock.getApplication(CoreService.CLOUD_GATEWAY.getServiceId())).thenReturn(null);
        when(discoveryClientMock.getApplication(CoreService.GATEWAY.getServiceId())).thenReturn(null);

        gatewayRegistry.cacheRefreshEventHandler(new CacheRefreshedEvent(), discoveryClientMock);
        assertNull(gatewayRegistry.knownAdditionalGateways.asMap().get(CGW_INSTANCE_ID));
        assertThat(gatewayRegistry.additionalGatewayIpAddressesReference.get().size(), is(0));
    }
}
