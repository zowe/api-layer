/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.services.status.event.model;

import org.zowe.apiml.apicatalog.model.APIService;
import lombok.Data;

import java.util.Set;

@Data
public class ContainerStatusChangeEvent implements StatusChangeEvent {
    private final String containerId;
    private final String title;
    private final String status;
    private final int totalServices;
    private final int activeServices;
    private Set<APIService> services;
    private final STATUS_EVENT_TYPE statusEventType;
    private final String timeStamp;

    public ContainerStatusChangeEvent(String containerId, String title, String status,
                                      int totalServices, int activeServices,
                                      Set<APIService> services,
                                      STATUS_EVENT_TYPE statusEventType) {
        this.containerId = containerId;
        this.title = title;
        this.status = status;
        this.services = services;
        this.totalServices = totalServices;
        this.activeServices = activeServices;
        this.statusEventType = statusEventType;
        timeStamp = setTimeStamp();
    }
}
