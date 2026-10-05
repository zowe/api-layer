/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.eurekaservice.client;

import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.client.RegistryTransport;
import org.zowe.apiml.registry.model.ServiceInstance;

/**
 * Hide the actual code for obtaining the registry client behind interface to simplify testing.
 * <p>
 * The name is kept from the Eureka-based enabler: it is a published extension point, and consumers that provide
 * their own {@link RegistryClient} - the Micronaut enabler wires one through this - keep compiling against it.
 */
public interface EurekaClientProvider {
    /**
     * Provide a registry client based on the provided configuration parameters.
     *
     * @param transport the transport to the Discovery Service
     * @param self      this service's own registration
     * @return Valid client for the Discovery service
     */
    RegistryClient client(RegistryTransport transport, ServiceInstance self);
}
