/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

import static org.zowe.apiml.constants.ApimlConstants.X_INSTANCEID;

/**
 * Custom class to handle deterministic load balancer routing based on the instance information provided in the request header X-InstanceId.
 */
@Slf4j
public class DeterministicLoadBalancer extends SameInstancePreferenceServiceInstanceListSupplier {

    @Value("${apiml.routing.instanceIdHeader:false}")
    private boolean instanceIdHeader;

    public DeterministicLoadBalancer(ServiceInstanceListSupplier delegate, ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
        super(delegate, factory);
        log.debug("DeterministicLoadBalancer instantiated");
    }

    /**
     * Retrieves the list of service instances, filtered by the 'X-InstanceId' header if present.
     *
     * @param request the load balancer request
     * @return a Flux of the filtered list of service instances
     */
    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
        if (instanceIdHeader) {
            return delegate.get(request)
                .map(serviceInstances -> filterInstances(request.getContext(), serviceInstances))
                .doOnError(e -> log.debug("Error in determining service instances", e));
        }
        return super.get();
    }

    private String getInstanceFromHeader(RequestDataContext context) {
        if (context != null && context.getClientRequest() != null) {
            HttpHeaders headers = context.getClientRequest().getHeaders();
            if (headers != null) {
                return headers.getFirst(X_INSTANCEID);
            }
        }
        return null;
    }

    /**
     * Retrieves the 'X-InstanceId' attribute from the request context.
     *
     * @param requestContext the request context
     * @return the instance ID, or null if not found
     */
    private String getInstanceId(Object requestContext) {
        if (requestContext instanceof RequestDataContext) {
            return getInstanceFromHeader((RequestDataContext) requestContext);
        }
        return null;
    }

    /**
     * Filters the list of service instances to include only those with the specified instance ID.
     *
     * @param requestContext the context
     * @param serviceInstances the list of service instances to filter
     * @return the filtered list of service instances
     */
    private List<ServiceInstance> filterInstances(Object requestContext, List<ServiceInstance> serviceInstances) {
        String instanceId = getInstanceId(requestContext);
        if (instanceId != null) {
            List<ServiceInstance> filteredInstances = serviceInstances.stream()
                .filter(instance -> instanceId.equals(instance.getInstanceId()))
                .collect(Collectors.toList());
            if (!filteredInstances.isEmpty()) {
                return filteredInstances;
            }
        }
        return serviceInstances;
    }
}
