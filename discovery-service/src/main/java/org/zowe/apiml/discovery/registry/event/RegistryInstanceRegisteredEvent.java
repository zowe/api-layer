/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.zowe.apiml.registry.model.ServiceInstance;

/**
 * Published when an instance is registered.
 * <p>
 * The Spring-flavoured counterpart of {@code RegistryEvent.InstanceRegistered}, replacing Spring Cloud's
 * {@code EurekaInstanceRegisteredEvent}. It exists because the Gateway rebuilds its route table off this signal
 * and the API Catalog tracks service availability from it - the registry core stays framework-free, and
 * {@link org.zowe.apiml.discovery.registry.SpringRegistryEventBridge} republishes.
 */
@Getter
public class RegistryInstanceRegisteredEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final ServiceInstance instance;
    private final boolean replication;

    public RegistryInstanceRegisteredEvent(Object source, ServiceInstance instance, boolean replication) {
        super(source);
        this.instance = instance;
        this.replication = replication;
    }

    /** Convenience for listeners that only care which service appeared. */
    public String getServiceId() {
        return instance == null ? null : instance.serviceId();
    }

}
