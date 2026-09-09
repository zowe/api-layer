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

import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.List;

/**
 * A read-only view of the registry, in the registry's own terms.
 * <p>
 * Distinct from Spring Cloud's {@code DiscoveryClient} on purpose. That interface models a host, a port, a
 * secure flag and a metadata map, which is exactly right for routing and load balancing and is what the
 * Gateway's route table uses. But {@code /gateway/services} and the API Catalog report a registration in
 * full - IP address, status page, health-check URL, lease, effective status - and those have nowhere to go on
 * Spring's interface.
 * <p>
 * The Eureka-era answer was to downcast: {@code ((EurekaServiceInstance) instance).getInstanceInfo()}, which
 * tied every such consumer to Netflix and broke the moment a different discovery client was in play. The
 * answer here is to ask for what is actually needed. Two implementations: the client's cached view, and the
 * registry itself for a process that holds one in the same JVM.
 */
public interface RegistryView {

    /** Registered service ids, lower-case. */
    List<String> serviceIds();

    /**
     * Every instance of a service, whatever its status.
     * <p>
     * Unlike {@code DiscoveryClient.getInstances}, which offers only instances that are up. A caller reporting
     * on the state of the mediation layer needs to see an instance that is down; a caller about to route a
     * request does not.
     */
    List<ServiceInstance> instances(String serviceId);

}
