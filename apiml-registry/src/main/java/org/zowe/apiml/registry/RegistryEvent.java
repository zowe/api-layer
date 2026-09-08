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

import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

/**
 * Something changed in the registry.
 * <p>
 * These mirror the Spring Cloud server events the existing code listens for - registration drives metadata
 * translation and defaults, and registry-available is what triggers static-definition loading - so the listeners
 * in discovery-service can be ported without changing their logic. Kept as plain types rather than Spring
 * {@code ApplicationEvent}s so the core stays framework-free; the Spring adapter republishes them.
 */
public sealed interface RegistryEvent {

    record InstanceRegistered(ServiceInstance instance, RegistrationKind kind, boolean fromPeer)
        implements RegistryEvent {
    }

    record InstanceCancelled(String appName, String instanceId, boolean fromPeer, boolean expired)
        implements RegistryEvent {
    }

    record InstanceRenewed(String appName, String instanceId, boolean fromPeer) implements RegistryEvent {
    }

    record StatusChanged(String appName, String instanceId, InstanceStatus status, boolean fromPeer)
        implements RegistryEvent {
    }

    /** Published once the registry is ready to serve and accept registrations. */
    record RegistryAvailable() implements RegistryEvent {
    }

}
