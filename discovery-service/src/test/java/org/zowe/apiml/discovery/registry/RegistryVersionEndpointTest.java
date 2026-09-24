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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.registry.InMemoryServiceRegistry;
import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.RegistrySettings;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The version the integration startup check reads on every instance, and the reason it is not a version.
 * <p>
 * {@code RegistryVersionEndpoint} reports the Eureka-compatible count of UP instances parsed out of the
 * applications hash code, not {@code Applications.version()}, which is the monotonic counter this registry
 * genuinely has available. Phase 5 is where that switches, and it has to switch on both sides at once: the
 * startup check compares the value <em>between</em> APIML instances to decide whether they have converged, and
 * the Caching Service, the discoverable client and {@code EurekaRegistryVersionEndpoint} still compute theirs
 * from a Eureka client's hash code. A Discovery Service reporting a monotonic counter while they report an
 * instance count would make every comparison meaningless - and the failure mode is not a test failure, it is
 * jobs hanging for three minutes in {@code areDiscoveryInSync()} and then failing with no explanation.
 * <p>
 * The client-side half of the same contract is pinned by
 * {@code RegistryClientVersionEndpointTest} in {@code apiml-registry-client-spring}.
 */
class RegistryVersionEndpointTest {

    private InMemoryServiceRegistry registry;
    private RegistryVersionEndpoint endpoint;

    private void freshRegistry() {
        registry = new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(), System::currentTimeMillis);
        endpoint = new RegistryVersionEndpoint(registry);
    }

    @Test
    @DisplayName("Three UP instances are reported as 3, the count - not as the registry's version")
    void thenTheUpCountIsReported() {
        freshRegistry();
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
        registry.register(instance("zaas", 10023), RegistrationKind.DYNAMIC);
        registry.register(instance("catalog", 10014), RegistrationKind.DYNAMIC);

        long reported = endpoint.status().getVersion();

        assertEquals(3L, reported, "the value is the count of UP instances parsed from the hash code");
        assertNotEquals(registry.applications().version(), reported,
            "reporting the monotonic version here would break the comparison against the Eureka-based clients");
    }

    @Test
    @DisplayName("An empty registry reports -1, which the startup check treats as 'not ready yet'")
    void thenAnEmptyRegistryIsNotReady() {
        freshRegistry();

        assertEquals(-1L, endpoint.status().getVersion());
    }

    /**
     * A DOWN instance is not counted: the hash code counts UP instances, and the startup check's convergence
     * test is about instances being registered and healthy, not about how many exist.
     */
    @Test
    @DisplayName("Only UP instances are counted, so a DOWN one does not raise the version")
    void thenOnlyUpInstancesAreCounted() {
        freshRegistry();
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
        registry.register(instance("zaas", 10023).toBuilder().status(InstanceStatus.DOWN).build(),
            RegistrationKind.DYNAMIC);

        assertEquals(1L, endpoint.status().getVersion());
    }

    private static ServiceInstance instance(String service, int port) {
        return ServiceInstance.builder()
            .instanceId("localhost:" + service + ":" + port)
            .appName(service.toUpperCase())
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(port, true)
            .vipAddress(service)
            .status(InstanceStatus.UP)
            .lease(Lease.renewable(30, 90, System.currentTimeMillis()))
            .build();
    }

}
