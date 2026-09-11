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

import org.springframework.context.ApplicationEvent;

/**
 * Published once the registry is open for traffic.
 * <p>
 * Replaces Spring Cloud's {@code EurekaRegistryAvailableEvent}. Static-definition loading and the Gateway health
 * indicator both hang off this.
 */
public class RegistryAvailableEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public RegistryAvailableEvent(Object source) {
        super(source);
    }

}
