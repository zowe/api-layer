/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry;

import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.List;
import java.util.Optional;

/**
 * The service registry.
 * <p>
 * Intentionally free of Spring, servlet and reactive types: the standalone Discovery Service is a servlet
 * application and the V4 modulith is WebFlux, and both have to sit on top of this. It is also free of static
 * accessors - the Eureka-based implementation reached the registry through
 * {@code EurekaServerContextHolder.getInstance()}, a global singleton that made the modulith's wiring awkward and
 * the behaviour hard to test. Callers get this injected.
 */
public interface ServiceRegistry extends RegistryView {

    /**
     * Add or replace an instance.
     *
     * @param instance the instance to store
     * @param kind     how it was registered; decides lease permanence and renew-threshold accounting
     */
    void register(ServiceInstance instance, RegistrationKind kind);

    /** Record a heartbeat. Returns false when the instance is not known, which tells the client to re-register. */
    boolean renew(String appName, String instanceId, boolean fromPeer);

    /** Remove an instance. Returns false when it was not there. */
    boolean cancel(String appName, String instanceId, boolean fromPeer);

    /** Set an operator status override. */
    boolean overrideStatus(String appName, String instanceId, InstanceStatus status, boolean fromPeer);

    /** Clear an operator status override, reverting to the instance's own reported status. */
    boolean clearStatusOverride(String appName, String instanceId, InstanceStatus revertTo, boolean fromPeer);

    /** Merge metadata into a registered instance, as used by PUT /eureka/apps/{app}/{id}/metadata. */
    boolean updateMetadata(String appName, String instanceId, java.util.Map<String, String> metadata);

    /** The whole registry. */
    Applications applications();

    /** Instances changed within the delta retention window. */
    Applications delta();

    Optional<Application> application(String appName);

    @Override
    default List<String> serviceIds() {
        return applications().applications().stream()
            .map(Application::name)
            .map(name -> name.toLowerCase(java.util.Locale.ROOT))
            .toList();
    }

    @Override
    default List<ServiceInstance> instances(String serviceId) {
        return application(serviceId)
            .map(Application::instances)
            .orElseGet(List::of);
    }

    Optional<ServiceInstance> instance(String appName, String instanceId);

    /** Every instance advertising the given VIP address, in registration order. */
    java.util.List<ServiceInstance> byVipAddress(String vipAddress);

    java.util.List<ServiceInstance> bySecureVipAddress(String secureVipAddress);

    /**
     * Expire lapsed leases.
     *
     * @param additionalLeaseMs slack added to every lease, to absorb GC pauses and clock drift
     * @return how many instances were evicted
     */
    int evict(long additionalLeaseMs);

    /** Whether eviction is currently permitted, i.e. self-preservation is not holding it back. */
    boolean evictionAllowed();

    int size();

    void addListener(RegistryListener listener);

    /** Marks the registry open for traffic and publishes {@link RegistryEvent.RegistryAvailable}. */
    void openForTraffic(int expectedClientsSendingRenews);

}
