/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.model;

import com.netflix.discovery.shared.Application;
import lombok.Data;

@Data
public class ServiceUpdateResult {
    Application service;
    SERVICE_UPDATE_TYPE updateType;

    public void addNewService(Application service) {
        this.service = service;
        this.updateType = SERVICE_UPDATE_TYPE.NEW_SERVICE;
    }

    public void addNewInstance(Application service) {
        this.service = service;
        this.updateType = SERVICE_UPDATE_TYPE.NEW_INSTANCE;
    }

    public void removeInstance(Application service) {
        this.service = service;
        this.updateType = SERVICE_UPDATE_TYPE.REMOVED_INSTANCE;
    }

    public void updateInstance(Application service) {
        this.service = service;
        this.updateType = SERVICE_UPDATE_TYPE.UPDATED_INSTANCE;
    }

    public void renewInstance(Application service) {
        this.service = service;
        this.updateType = SERVICE_UPDATE_TYPE.RENEW_INSTANCE;
    }

    public enum SERVICE_UPDATE_TYPE {
        NEW_SERVICE,
        NEW_INSTANCE,
        REMOVED_INSTANCE,
        UPDATED_INSTANCE,
        RENEW_INSTANCE,
        CREATED_CONTAINER
    }
}
