/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;

/**
 * Exposes a {@link RegistryClient}'s cached view as a Spring Cloud {@link DiscoveryClient}.
 * <p>
 * This is what keeps the change invisible to the Gateway: Spring Cloud Gateway, the load balancer and the API
 * Catalog all consume {@code DiscoveryClient}, so replacing what sits behind it does not touch route building.
 * Only UP instances are returned, so an instance an operator has taken out of service stops receiving traffic.
 */
public class CachedRegistryDiscoveryClient implements DiscoveryClient {

    private final RegistryClient client;

    public CachedRegistryDiscoveryClient(RegistryClient client) {
        this.client = client;
    }

    @Override
    public String description() {
        return "APIML registry client";
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        return client.cache().upInstances(serviceId).stream()
            .map(CachedRegistryDiscoveryClient::toSpringInstance)
            .toList();
    }

    @Override
    public List<String> getServices() {
        return client.cache().serviceIds();
    }

    static ServiceInstance toSpringInstance(org.zowe.apiml.registry.model.ServiceInstance instance) {
        boolean secure = instance.securePort() != null && instance.securePort().enabled();
        int port = secure ? instance.securePort().port() : instance.port().port();
        return new DefaultServiceInstance(
            instance.instanceId(),
            instance.serviceId(),
            instance.hostName(),
            port,
            secure,
            instance.metadata()
        );
    }

}
