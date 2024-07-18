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
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ResponseStatusException;
import org.zowe.apiml.gateway.caching.LoadBalancerCache;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public class StickySessionLoadBalancer extends SameInstancePreferenceServiceInstanceListSupplier {

    private final LoadBalancerCache cache;
    private final int expirationTime;
    public StickySessionLoadBalancer(ServiceInstanceListSupplier delegate, ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory, LoadBalancerCache cache, int expirationTime) {
        super(delegate, loadBalancerClientFactory);
        this.cache = cache;
        this.expirationTime = expirationTime;
        log.debug("StickySessionLoadBalancer instantiated");
    }

    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
        String serviceId = getServiceId();
        if (serviceId == null) {
            return Flux.empty();
        }
        AtomicReference<String> principal = new AtomicReference<>();
        return delegate.get(request)
            .flatMap(serviceInstances -> getPrincipal().flatMapMany(user -> {
                if (user.isEmpty()) {
                    log.debug("No authentication present on request, not filtering the service: {}", serviceId);
                    return Flux.empty();
                } else {
                    principal.set(user);
                    return cache.retrieve(user, serviceId);
                }
            }).flatMap(record -> {
                List<ServiceInstance> filteredInstances = filterInstances(record, serviceInstances);
                if (filteredInstances.isEmpty()) {
                    log.warn("No service instance found for the provided instance ID");
                    return Flux.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Service instance not found for the provided instance ID"));
                }
                var result = Flux.just(filteredInstances);

                if (isTooOld(record.getCreationTime())) {
                    result = cache.delete(principal.get(), serviceId).thenMany(Flux.just(filteredInstances));
                }
                return result;
            }).defaultIfEmpty(serviceInstances))
            .doOnError(e -> log.debug("Error in determining service instances", e));
    }

    private boolean isTooOld(LocalDateTime cachedDate) {
        LocalDateTime now = LocalDateTime.now().minusHours(expirationTime);
        return now.isAfter(cachedDate);
    }

    public static Mono<String> getPrincipal() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getPrincipal)
            .filter(principal -> principal != null && !principal.toString().isEmpty())
            .map(Object::toString);
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
