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

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.DiscoveryMetadata;
import org.zowe.apiml.registry.model.InstanceStatus;

import java.util.List;
import java.util.Map;

/**
 * Exposes the local registry as a Spring Cloud {@link DiscoveryClient}.
 * <p>
 * The health indicator and anything else asking "is ZAAS up?" go through this rather than through a Eureka client
 * that would talk HTTP to this very process. Only instances that are actually routable are returned, so a health
 * check does not report a dependency as available while an operator has taken it out of service.
 */
@Component
@RequiredArgsConstructor
public class RegistryDiscoveryClient implements DiscoveryClient {

    private final ServiceRegistry registry;

    @Override
    public String description() {
        return "APIML registry (local)";
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        return registry.application(serviceId)
            .map(Application::instances)
            .orElseGet(List::of)
            .stream()
            .filter(instance -> instance.effectiveStatus() == InstanceStatus.UP)
            .map(this::toSpringInstance)
            .toList();
    }

    @Override
    public List<String> getServices() {
        return registry.applications().applications().stream()
            .map(Application::name)
            .map(String::toLowerCase)
            .toList();
    }

    private ServiceInstance toSpringInstance(org.zowe.apiml.registry.model.ServiceInstance instance) {
        boolean secure = instance.securePort() != null && instance.securePort().enabled();
        int port = secure ? instance.securePort().port() : instance.port().port();
        // Carry the advertised home page through: Spring's ServiceInstance has nowhere to put it, and the API
        // Catalog needs the full URL including its path. See DiscoveryMetadata.
        var metadata = new java.util.LinkedHashMap<>(instance.metadata());
        if (instance.homePageUrl() != null) {
            metadata.put(DiscoveryMetadata.HOME_PAGE_URL, instance.homePageUrl());
        }
        metadata.put(DiscoveryMetadata.INSTANCE_STATUS, instance.effectiveStatus().name());

        return new SchemeAwareServiceInstance(
            instance.instanceId(),
            instance.serviceId(),
            instance.hostName(),
            port,
            secure,
            metadata
        );
    }

    /** DefaultServiceInstance leaves ServiceInstance.getScheme() as null; Eureka's adapter did not. */
    private static final class SchemeAwareServiceInstance extends DefaultServiceInstance {

        private SchemeAwareServiceInstance(
            String instanceId,
            String serviceId,
            String host,
            int port,
            boolean secure,
            Map<String, String> metadata
        ) {
            super(instanceId, serviceId, host, port, secure, metadata);
        }

        @Override
        public String getScheme() {
            return isSecure() ? "https" : "http";
        }

    }

}
