/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.proxyheaders;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.registry.client.RegistryCache;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.client.RegistryTransport;
import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.PortInfo;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class AdditionalRegistrationGatewayRegistryTest {

    private AdditionalRegistrationGatewayRegistry gatewayRegistry;

    // Gateway with single IP address
    private final String GW1_INSTANCE_ID = "gw1-instance-id";
    private final String GW1_IP_ADDRESS = "2.2.2.2";
    private final String GW1_HOSTNAME = "gw1-hostname";

    private final ServiceInstance GW1_INSTANCE = gateway(GW1_INSTANCE_ID, GW1_HOSTNAME, GW1_IP_ADDRESS);

    // Gateway resolved to multiple ip addresses
    private final String GW2_INSTANCE_ID = "gw2-instance-id";
    private final String GW2_IP_ADDRESS = "3.3.3.3";
    private final String GW2_IP_ADDRESS_FROM_DNS = "4.4.4.4";
    private final String GW2_HOSTNAME = "gw2-hostname";

    private final ServiceInstance GW2_INSTANCE = gateway(GW2_INSTANCE_ID, GW2_HOSTNAME, GW2_IP_ADDRESS);

    private static ServiceInstance gateway(String instanceId, String hostname, String ipAddress) {
        return ServiceInstance.builder()
            .appName(CoreService.GATEWAY.getServiceId())
            .instanceId(instanceId)
            .hostName(hostname)
            .ipAddr(ipAddress)
            .port(new PortInfo(10010, true))
            .securePort(new PortInfo(10010, false))
            .status(InstanceStatus.UP)
            .build();
    }

    /** The view an additional registration's client would have after a refresh. */
    private static RegistryCache viewOf(ServiceInstance... instances) {
        RegistryCache cache = new RegistryCache();
        List<Application> applications = instances.length == 0
            ? List.of()
            : List.of(new Application(CoreService.GATEWAY.getServiceId().toUpperCase(), List.of(instances)));
        cache.replace(new Applications(applications, 1L, Applications.computeHashCode(applications)));
        return cache;
    }

    @BeforeEach
    @SneakyThrows
    void setup() {
        gatewayRegistry = new AdditionalRegistrationGatewayRegistry();
        gatewayRegistry.registryExpiration = Duration.ofSeconds(300);
        gatewayRegistry.init();
    }

    /**
     * Previously asserted by verifying {@code registerEventListener} was called on a mocked Eureka client. The
     * listener is now an ordinary callback, so this drives the real path instead: subscribe, refresh, and check
     * the addresses arrived.
     */
    @Test
    @DisplayName("Then subscribing to a client means a refresh reaches the registry")
    void listenerIsSubscribedToTheClient() {
        var client = new RegistryClient(new FixedTransport(viewOf(GW1_INSTANCE).applications()));

        gatewayRegistry.watch(client);
        client.refresh();

        assertTrue(gatewayRegistry.getAdditionalGatewayIpAddressesReference().get().contains(GW1_IP_ADDRESS));
    }

    @Test
    void whenGateway_thenAddIpToRegistry() {
        gatewayRegistry.onRefresh(viewOf(GW1_INSTANCE));

        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().size(), is(1));
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(GW1_INSTANCE_ID), is(List.of(GW1_IP_ADDRESS)));

        assertThat(gatewayRegistry.additionalGatewayIpAddressesReference.get().size(), is(1));
        assertTrue(gatewayRegistry.additionalGatewayIpAddressesReference.get().contains(GW1_IP_ADDRESS));
    }

    @Test
    void whenGatewayWithDNSResolution_thenAddIpToRegistry() throws UnknownHostException {
        InetAddress[] resolvedGw2Hostname = new InetAddress[]{InetAddress.getByName(GW2_IP_ADDRESS_FROM_DNS)};
        InetAddress[] resolvedGw2IpAddress = new InetAddress[]{InetAddress.getByName(GW2_IP_ADDRESS)};

        try (MockedStatic<InetAddress> inetAddressMocked = Mockito.mockStatic(InetAddress.class)) {
            // We cannot use .callReaLMethod() as it relies on native call that cannot be used via mock
            inetAddressMocked.when(() -> InetAddress.getAllByName(GW2_IP_ADDRESS)).thenReturn(resolvedGw2IpAddress);
            inetAddressMocked.when(() -> InetAddress.getAllByName(GW2_HOSTNAME)).thenReturn(resolvedGw2Hostname);

            gatewayRegistry.onRefresh(viewOf(GW2_INSTANCE));
        }

        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().size(), is(1));
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(GW2_INSTANCE_ID).size(), is(2));
        assertTrue(gatewayRegistry.knownAdditionalGateways.asMap().get(GW2_INSTANCE_ID).containsAll(List.of(GW2_IP_ADDRESS, GW2_IP_ADDRESS_FROM_DNS)));

        assertThat(gatewayRegistry.additionalGatewayIpAddressesReference.get().size(), is(2));
        assertTrue(gatewayRegistry.additionalGatewayIpAddressesReference.get().containsAll(List.of(GW2_IP_ADDRESS, GW2_IP_ADDRESS_FROM_DNS)));
    }

    @Test
    void whenGatewayWithDNSResolutionFailed_thenAddIpToRegistry() throws UnknownHostException {
        InetAddress[] resolvedGw2IpAddress = new InetAddress[]{InetAddress.getByName(GW2_IP_ADDRESS)};

        try (MockedStatic<InetAddress> inetAddressMocked = Mockito.mockStatic(InetAddress.class)) {
            // We cannot use .callReaLMethod() as it relies on native call that cannot be used via mock
            inetAddressMocked.when(() -> InetAddress.getAllByName(GW2_IP_ADDRESS)).thenReturn(resolvedGw2IpAddress);
            inetAddressMocked.when(() -> InetAddress.getAllByName(GW2_HOSTNAME)).thenThrow(new UnknownHostException());

            gatewayRegistry.onRefresh(viewOf(GW2_INSTANCE));
        }

        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().size(), is(1));
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(GW2_INSTANCE_ID).size(), is(1));
        assertTrue(gatewayRegistry.knownAdditionalGateways.asMap().get(GW2_INSTANCE_ID).contains(GW2_IP_ADDRESS));

        assertThat(gatewayRegistry.additionalGatewayIpAddressesReference.get().size(), is(1));
        assertTrue(gatewayRegistry.additionalGatewayIpAddressesReference.get().contains(GW2_IP_ADDRESS));
    }

    @Test
    void whenMultipleGateways_thenAddIpToRegistry() {
        gatewayRegistry.onRefresh(viewOf(GW1_INSTANCE, GW2_INSTANCE));

        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().size(), is(2));
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(GW1_INSTANCE_ID), is(List.of(GW1_IP_ADDRESS)));
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(GW2_INSTANCE_ID), is(List.of(GW2_IP_ADDRESS)));

        assertThat(gatewayRegistry.additionalGatewayIpAddressesReference.get().size(), is(2));
        assertTrue(gatewayRegistry.additionalGatewayIpAddressesReference.get().containsAll(List.of(GW1_IP_ADDRESS, GW2_IP_ADDRESS)));
    }

    @Test
    void clearGwRegistryCache() {
        gatewayRegistry.registryExpiration = Duration.ofSeconds(1);
        gatewayRegistry.init();

        whenGateway_thenAddIpToRegistry();
        assertThat(gatewayRegistry.knownAdditionalGateways.asMap().get(GW1_INSTANCE_ID).size(), is(1));

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
            // We cannot validate on size as the entries may still be in the cache but masked
            assertNull(gatewayRegistry.knownAdditionalGateways.asMap().get(GW1_INSTANCE_ID))
        );

        gatewayRegistry.onRefresh(viewOf());

        assertNull(gatewayRegistry.knownAdditionalGateways.asMap().get(GW1_INSTANCE_ID));
        assertThat(gatewayRegistry.additionalGatewayIpAddressesReference.get().size(), is(0));
    }

    /** Serves one fixed view, so {@link RegistryClient#refresh()} can be driven without a Discovery Service. */
    private record FixedTransport(Applications applications) implements RegistryTransport {

        @Override
        public Applications fetchApplications() {
            return applications;
        }

        @Override
        public Applications fetchDelta() {
            return null;
        }

        @Override
        public void register(ServiceInstance instance) {
            // read-only in these cases
        }

        @Override
        public boolean renew(String appName, String instanceId) {
            return true;
        }

        @Override
        public void cancel(String appName, String instanceId) {
            // read-only in these cases
        }

        @Override
        public void updateStatus(String appName, String instanceId, InstanceStatus status) {
            // read-only in these cases
        }

    }

}
