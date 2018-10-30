/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(value = "info")
public class InstanceInfo {
    private App app;
    private MFaasInfo mfaasInfo;

    public InstanceInfo(App app, MFaasInfo mfaaSInfo) {
        this.app = app;
        this.mfaasInfo = mfaaSInfo;
    }

    public App getApp() {
        return app;
    }

    public MFaasInfo getMFaaSInfo() {
        return mfaasInfo;
    }
}
