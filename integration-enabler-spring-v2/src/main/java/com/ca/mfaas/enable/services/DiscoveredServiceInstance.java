/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.enable.services;

import com.netflix.appinfo.InstanceInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class DiscoveredServiceInstance {
    List<InstanceInfo> instanceInfos = new ArrayList<>();
    List<ServiceInstance> serviceInstances = new ArrayList<>();

    public boolean hasInstances() {
        return !instanceInfos.isEmpty() || !serviceInstances.isEmpty();
    }
}
