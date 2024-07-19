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
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.zowe.apiml.gateway.caching.LoadBalancerCache;
import org.zowe.apiml.gateway.filters.DistributedLoadBalancerFilterFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static reactor.core.publisher.Flux.just;
import static reactor.core.publisher.Mono.empty;

/**
 * A sticky session load balancer that ensures requests from the same user are routed to the same service instance.
 */
@Slf4j
public class StickySessionLoadBalancer extends SameInstancePreferenceServiceInstanceListSupplier {

    private final LoadBalancerCache cache;
    private final int expirationTime;

    public StickySessionLoadBalancer(ServiceInstanceListSupplier delegate,
                                     ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory,
                                     LoadBalancerCache cache,
                                     int expirationTime) {
        super(delegate, loadBalancerClientFactory);
        this.cache = cache;
        this.expirationTime = expirationTime;
        log.debug("StickySessionLoadBalancer instantiated");
    }

    /**
     * Gets a list of service instances based on the request. This method ensures that requests from the same user are
     * routed to the same service instance, leveraging the cache to maintain sticky sessions.
     *
     * @param request the load balancer request
     * @return a flux of service instance lists
     */
    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
        String serviceId = getServiceId();
        if (serviceId == null) {
            return Flux.empty();
        }
        AtomicReference<String> principal = new AtomicReference<>();
        return delegate.get(request)
            .flatMap(serviceInstances -> getSub(request.getContext())
                .switchIfEmpty(Mono.just(""))
                .flatMap(user -> {
                    if (user == null || user.isEmpty()) {
                        log.debug("No authentication present on request, not filtering the service: {}", serviceId);
                        return empty();
                    } else {
                        principal.set(user);
                        return cache.retrieve(user, serviceId).onErrorResume(t -> Mono.empty());
                    }
                }).switchIfEmpty(Mono.just(LoadBalancerCache.LoadBalancerCacheRecord.NONE))
            .flatMapMany(record -> {
                if (record == LoadBalancerCache.LoadBalancerCacheRecord.NONE) {
                    return empty();
                }
                List<ServiceInstance> filteredInstances = filterInstances(record, serviceInstances);
                if (filteredInstances.isEmpty()) {
                    log.debug("No cached information found, the original service instances will be used for the load balancing");
                    return just(serviceInstances);
                }
                var result = just(filteredInstances);

                if (isTooOld(record.getCreationTime())) {
                    result = cache.delete(principal.get(), serviceId).thenMany(just(filteredInstances));
                }
                return result;
            }).switchIfEmpty(Mono.just(serviceInstances)))
            .doOnError(e -> log.debug("Error in determining service instances", e));
    }

    /**
     * Checks if the cached date is too old based on the expiration time.
     *
     * @param cachedDate the cached date
     * @return true if the cached date is too old, false otherwise
     */
    private boolean isTooOld(LocalDateTime cachedDate) {
        LocalDateTime now = LocalDateTime.now().minusHours(expirationTime);
        return now.isAfter(cachedDate);
    }

    private Mono<String> getSub(Object requestContext) {
        if (requestContext instanceof RequestDataContext ctx) {
            // TODO cookie might not be there
            var token = ctx.getClientRequest().getCookies().get("apimlAuthenticationToken").get(0);
            return Mono.just(DistributedLoadBalancerFilterFactory.extractSubFromToken(token));
        }
        return Mono.just("");
    }

    /**
     * Filters the list of service instances to include only those with the specified instance ID.
     *
     * @param record the cache record
     * @param serviceInstances the list of service instances to filter
     * @return the filtered list of service instances
     */
    private List<ServiceInstance> filterInstances(LoadBalancerCache.LoadBalancerCacheRecord record, List<ServiceInstance> serviceInstances) {
        if (record.getInstanceId() != null) {
            List<ServiceInstance> filteredInstances = serviceInstances.stream()
                .filter(instance -> record.getInstanceId().equals(instance.getInstanceId()))
                .collect(Collectors.toList());
            if (!filteredInstances.isEmpty()) {
                return filteredInstances;
            }
        }
        return new ArrayList<>();
    }
}
