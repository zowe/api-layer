/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.zowe.apiml.constants.ApimlConstants.X_INSTANCEID;

/**
 * Custom class to handle deterministic load balancer routing based on the instance information provided in the request header X-InstanceId.
 */
@Slf4j
public class DeterministicLoadBalancer extends SameInstancePreferenceServiceInstanceListSupplier {

    public DeterministicLoadBalancer(ServiceInstanceListSupplier delegate, ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
        super(delegate, factory);
        log.debug("DeterministicLoadBalancer instantiated");
    }

    /**
     * Retrieves the list of service instances, filtered by the 'X-InstanceId' header if present.
     *
     * @param request the load balancer request
     * @return a Flux of the filtered list of service instances, otherwise returns 404 message
     */
    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
        return delegate.get(request)
            .flatMap(serviceInstances -> {
                var instanceId = getInstanceId(request.getContext());
                var filteredInstances = serviceInstances;
                if (!StringUtils.isEmpty(instanceId)) {
                    filteredInstances = filterInstances(instanceId, serviceInstances);
                    if (filteredInstances.isEmpty()) {
                        log.warn("No service instance found for the provided instance ID");
                        return Flux.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Service instance not found for the provided instance ID"));
                    }
                }
                return Flux.just(filteredInstances);
            })
            .doOnError(e -> log.debug("Error in determining service instances", e));
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
     * @param instanceId       ID of the service instance
     * @param serviceInstances the list of service instances to filter
     * @return the filtered list of service instances
     */
    private List<ServiceInstance> filterInstances(String instanceId, List<ServiceInstance> serviceInstances) {

        if (instanceId != null) {
            List<ServiceInstance> filteredInstances = serviceInstances.stream()
                .filter(instance -> instanceId.equals(instance.getInstanceId()))
                .collect(Collectors.toList());
            if (!filteredInstances.isEmpty()) {
                return filteredInstances;
            }
        }
        return new ArrayList<>();
    }
}
