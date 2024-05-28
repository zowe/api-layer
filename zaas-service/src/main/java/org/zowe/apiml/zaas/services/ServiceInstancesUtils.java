/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.services;

import com.netflix.zuul.context.RequestContext;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

public class ServiceInstancesUtils {

    private ServiceInstancesUtils() {}

    public static List<ServiceInstance> getServiceInstancesFromDiscoveryClient(DiscoveryClient discoveryClient) {
        RequestContext context = RequestContext.getCurrentContext();
        String serviceId = (String) context.get(SERVICE_ID_KEY);
        return discoveryClient.getInstances(serviceId);
    }
}
