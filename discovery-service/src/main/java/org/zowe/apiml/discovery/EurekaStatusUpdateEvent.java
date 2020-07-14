/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discovery;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EurekaStatusUpdateEvent extends ApplicationEvent {

    private String serviceId;
    private String instanceId;
    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public EurekaStatusUpdateEvent(Object source, String serviceId, String instanceId) {
        super(source);
        this.serviceId = serviceId;
        this.instanceId = instanceId;
    }
}
