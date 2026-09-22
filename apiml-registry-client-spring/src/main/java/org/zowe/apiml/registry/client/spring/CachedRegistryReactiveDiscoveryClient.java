/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.zowe.apiml.registry.client.CachedRegistryDiscoveryClient;
import org.zowe.apiml.registry.client.RegistryClient;
import reactor.core.publisher.Flux;

/**
 * The registry, as a Spring Cloud {@link ReactiveDiscoveryClient}.
 * <p>
 * The reactive counterpart of {@link CachedRegistryDiscoveryClient}, over the same cache. A reactive application
 * cannot use the blocking one: Spring Cloud Gateway's route building, the CORS updater and the ZAAS scheme filter
 * all take {@code ReactiveDiscoveryClient}, and without a registry-backed implementation each of them falls back
 * to Spring Cloud's {@code SimpleReactiveDiscoveryClient}, which is empty unless services are listed in
 * configuration.
 * <p>
 * The Gateway is the case that matters. {@code RouteLocator.getServiceInstances()} enumerates
 * {@code getServices()} and builds a route per service, so an empty client does not degrade routing, it removes
 * it: the Gateway starts, registers and reports healthy, and routes nothing.
 */
public class CachedRegistryReactiveDiscoveryClient implements ReactiveDiscoveryClient {

    private final RegistryClient client;

    public CachedRegistryReactiveDiscoveryClient(RegistryClient client) {
        this.client = client;
    }

    @Override
    public String description() {
        return "APIML registry reactive client";
    }

    @Override
    public Flux<ServiceInstance> getInstances(String serviceId) {
        return Flux.fromIterable(client.cache().upInstances(serviceId))
            .map(CachedRegistryDiscoveryClient::toSpringInstance);
    }

    @Override
    public Flux<String> getServices() {
        return Flux.fromIterable(client.cache().serviceIds());
    }

}
