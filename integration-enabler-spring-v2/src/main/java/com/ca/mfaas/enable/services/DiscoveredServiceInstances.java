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

import java.util.HashMap;
import java.util.Map;

public class DiscoveredServiceInstances {

    private Map<String, DiscoveredServiceInstance> instances = new HashMap<>();

    public Map<String, DiscoveredServiceInstance> getAllInstances() {
        return instances;
    }

    public DiscoveredServiceInstance getInstancesForService(String serviceId) {
        return instances.get(serviceId);
    }

    public void addInstancesForServiceId(String serviceId, DiscoveredServiceInstance serviceInstances) {
        if (instances.get(serviceId) == null) {
            instances.put(serviceId, serviceInstances);
        } else {
            throw new IllegalStateException("Attempting to add instances for an existing serviceId: " + serviceId
                + ". There should only be one DiscoveredServiceInstance per serviceId.");
        }
    }
}
