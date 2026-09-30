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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.product.eureka.DomainAllowListMetadataException;
import org.zowe.apiml.registry.InMemoryServiceRegistry;
import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.RegistrySettings;
import org.zowe.apiml.registry.codec.RegistryCodec;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.registry.spi.RegistrationInterceptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The metadata write path, at the boundary the two callers share.
 * <p>
 * Kept separate from the registry's own tests because the decision being pinned here is the HTTP one: the
 * registry refuses the write, and this layer turns that refusal into the status the endpoint has always
 * answered. The registration path answers 400 through {@link RegistryExceptionHandler}; this path answers 500,
 * which is what {@code DiscoverableClientIntegrationTest.whenUpdateMetadataWithUrlNotInAllowList_thenReject}
 * asserts.
 */
class RegistryEndpointsUpdateMetadataTest {

    private static final String INSTANCE_ID = "localhost:registrationtest:10013";

    private InMemoryServiceRegistry registry;
    private RegistryEndpoints endpoints;

    @BeforeEach
    void setUp() {
        RegistrationInterceptor allowList = instance -> {
            boolean disallowed = instance.metadata().values().stream()
                .anyMatch(value -> value.contains("baddomain.net"));
            if (disallowed) {
                throw new DomainAllowListMetadataException(
                    "URLs not allowed found for instance " + instance.instanceId());
            }
            return instance;
        };
        registry = new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(allowList),
            System::currentTimeMillis);
        registry.register(instance(), RegistrationKind.DYNAMIC);
        endpoints = new RegistryEndpoints(registry, new RegistryCodec());
    }

    @Test
    void givenAllowedMetadata_whenUpdated_thenAccepted() {
        RegistryEndpoints.Result result = endpoints.updateMetadata("REGISTRATIONTEST", INSTANCE_ID,
            Map.of("apiml.externalUrl", "https://www.zowe.org"));

        assertEquals(200, result.status());
    }

    /**
     * The case that failed: the allow list is consulted on a metadata update, and a disallowed URL is refused
     * with the 500 this endpoint has always returned.
     */
    @Test
    void givenDisallowedMetadata_whenUpdated_thenRejectedUntouched() {
        RegistryEndpoints.Result result = endpoints.updateMetadata("REGISTRATIONTEST", INSTANCE_ID,
            Map.of("apiml.externalUrl", "https://baddomain.net"));

        assertEquals(500, result.status());
        assertEquals(false, registry.instance("REGISTRATIONTEST", INSTANCE_ID).orElseThrow()
            .metadata().containsKey("apiml.externalUrl"));
    }

    @Test
    void givenUnknownInstance_whenUpdated_thenNotFound() {
        assertEquals(404, endpoints.updateMetadata("REGISTRATIONTEST", "localhost:nope:1", Map.of()).status());
    }

    private static ServiceInstance instance() {
        return ServiceInstance.builder()
            .instanceId(INSTANCE_ID)
            .appName("REGISTRATIONTEST")
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(10013, true)
            .vipAddress("registrationtest")
            .status(InstanceStatus.UP)
            .lease(Lease.renewable(30, 90, System.currentTimeMillis()))
            .build();
    }

}
