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

import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

/**
 * Reads and writes a registry that lives in the same JVM.
 * <p>
 * This is the modulith's transport, and it is the point of the exercise. The Eureka arrangement ran a full
 * {@code EurekaClient} inside the modulith that polled {@code /eureka/apps} over HTTP on the process's own port -
 * serialising the registry to JSON, sending it through the loopback interface and parsing it again, purely to
 * read a data structure already in memory. It also did not work cleanly: the modulith restricts
 * {@code /eureka/**} to the internal Discovery port, so a client configured against the Gateway port was refused
 * and retried on a timer.
 * <p>
 * Here there is no socket, no serialisation and nothing to authenticate.
 */
public final class LocalRegistryTransport implements RegistryTransport {

    private final ServiceRegistry registry;

    public LocalRegistryTransport(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Applications fetchApplications() {
        return registry.applications();
    }

    /**
     * Always null: a local client has no reason to reconcile deltas.
     * <p>
     * Deltas exist to avoid shipping the whole registry over a network. Reading the full snapshot from memory is
     * cheaper than folding a delta into a cached copy, and it cannot drift.
     */
    @Override
    public Applications fetchDelta() {
        return null;
    }

    @Override
    public void register(ServiceInstance instance) {
        registry.register(instance, RegistrationKind.DYNAMIC);
    }

    @Override
    public boolean renew(String appName, String instanceId) {
        return registry.renew(appName, instanceId, false);
    }

    @Override
    public void cancel(String appName, String instanceId) {
        registry.cancel(appName, instanceId, false);
    }

    @Override
    public void updateStatus(String appName, String instanceId, InstanceStatus status) {
        registry.overrideStatus(appName, instanceId, status, false);
    }

}
