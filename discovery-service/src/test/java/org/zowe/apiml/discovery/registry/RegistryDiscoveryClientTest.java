/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.registry.InMemoryServiceRegistry;
import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.RegistrySettings;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistryDiscoveryClientTest {

    private static final long NOW = 1_700_000_000_000L;

    @Test
    void springInstancesExposeTheirTransportScheme() {
        var registry = new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(), () -> NOW);
        registry.register(instance("secure", 10010, true), RegistrationKind.DYNAMIC);
        registry.register(instance("nonsecure", 10011, false), RegistrationKind.DYNAMIC);

        var client = new RegistryDiscoveryClient(registry);

        assertEquals("https", client.getInstances("secure").get(0).getScheme());
        assertEquals("http", client.getInstances("nonsecure").get(0).getScheme());
    }

    private ServiceInstance instance(String serviceId, int port, boolean secure) {
        return ServiceInstance.builder()
            .instanceId("localhost:" + serviceId + ":" + port)
            .appName(serviceId)
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(port, !secure)
            .securePort(port, secure)
            .status(InstanceStatus.UP)
            .lease(Lease.renewable(30, 90, NOW))
            .build();
    }

}
