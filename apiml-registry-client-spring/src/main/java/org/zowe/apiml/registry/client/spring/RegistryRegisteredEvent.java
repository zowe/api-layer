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

import org.springframework.context.ApplicationEvent;
import org.zowe.apiml.registry.model.ServiceInstance;

/** Published once this service's own registration has been accepted by a Discovery Service. */
public class RegistryRegisteredEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final transient ServiceInstance instance;

    public RegistryRegisteredEvent(Object source, ServiceInstance instance) {
        super(source);
        this.instance = instance;
    }

    public ServiceInstance getInstance() {
        return instance;
    }

}
