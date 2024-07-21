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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.zowe.apiml.gateway.caching.LoadBalancerCache;
import org.zowe.apiml.gateway.caching.LoadBalancerCache.LoadBalancerCacheRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static reactor.core.publisher.Flux.just;
import static reactor.core.publisher.Mono.empty;

/**
 * A sticky session load balancer that ensures requests from the same user are routed to the same service instance.
 */
@Slf4j
public class StickySessionLoadBalancer extends SameInstancePreferenceServiceInstanceListSupplier {

    private static final String HEADER_NONE_SIGNATURE = Base64.getEncoder().encodeToString("""
        {"typ":"JWT","alg":"none"}""".getBytes(StandardCharsets.UTF_8));

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
                })
                .switchIfEmpty(Mono.just(LoadBalancerCache.LoadBalancerCacheRecord.NONE))
                .flatMapMany(record -> filterInstances(principal.get(), serviceId, record, serviceInstances))
            )
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
            var token = Optional.ofNullable(ctx.getClientRequest().getCookies().get("apimlAuthenticationToken")).map(list -> list.get(0)).orElse("");
            return Mono.just(extractSubFromToken(token));
        }
        return Mono.just("");
    }

    /**
     * Filters the list of service instances to include only those with the specified instance ID.
     *
     * @param user
     * @param serviceId
     * @param record the cache record
     * @param serviceInstances the list of service instances to filter
     *
     * @return the filtered list of service instances
     */
    private Flux<List<ServiceInstance>> filterInstances(
            String user,
            String serviceId,
            LoadBalancerCacheRecord record,
            List<ServiceInstance> serviceInstances) {

        Flux<List<ServiceInstance>> result = just(serviceInstances);
        if (shouldIgnore(serviceInstances)) {
            return result;
        }
        if (isNotBlank(record.getInstanceId()) && isTooOld(record.getCreationTime())) {
            result = cache.delete(user, serviceId)
                .thenMany(chooseOne(user, serviceInstances));
        } else if (isNotBlank(record.getInstanceId())) {
            result = chooseOne(record.getInstanceId(), user, serviceInstances);
        } else {
            result = chooseOne(user, serviceInstances);
        }
        return result;
    }

    /**
     * Selected the preferred instance if not null, if the preferred instance is not found or is null a new preference is created
     *
     * @param instanceId The preferred instanceId
     * @param user The user
     * @param serviceInstances The default serviceInstances available
     * @return Flux with a list containing only one selected instance
     */
    private Flux<List<ServiceInstance>> chooseOne(String instanceId, String user, List<ServiceInstance> serviceInstances) {
        Stream<ServiceInstance> stream = serviceInstances.stream();
        if (instanceId != null) {
            stream = stream.filter(instance -> instanceId.equals(instance.getInstanceId()));
        }
        ServiceInstance chosenInstance = stream.findAny().orElse(serviceInstances.get(0));
        return cache.store(user, chosenInstance.getServiceId(), new LoadBalancerCacheRecord(chosenInstance.getInstanceId()))
            .thenMany(just(Collections.singletonList(chosenInstance)));
    }

    /**
     * Shortcut to create a new instance preference
     *
     * @param user
     * @param serviceInstances
     * @return
     */
    private Flux<List<ServiceInstance>> chooseOne(String user, List<ServiceInstance> serviceInstances) {
        return chooseOne(null, user, serviceInstances);
    }

    boolean shouldIgnore(List<ServiceInstance> instances) {
        return instances.isEmpty() || !lbTypeIsAuthentication(instances.get(0));
    }

    private boolean lbTypeIsAuthentication(ServiceInstance instance) {
        Map<String, String> metadata = instance.getMetadata();
        if (metadata != null) {
            String lbType = metadata.get("apiml.lb.type");
            return lbType != null && lbType.equals("authentication");
        }
        return false;
    }

    private static String removeJwtSign(String jwtToken) {
        if (jwtToken == null) return null;

        int firstDot = jwtToken.indexOf('.');
        int lastDot = jwtToken.lastIndexOf('.');
        if ((firstDot < 0) || (firstDot >= lastDot)) throw new MalformedJwtException("Invalid JWT format");

        return HEADER_NONE_SIGNATURE + jwtToken.substring(firstDot, lastDot + 1);
    }

    private static Claims getJwtClaims(String jwt) {
        /*
         * Removes signature, because we don't have key to verify z/OS tokens, and we just need to read claim.
         * Verification is done by SAF itself. JWT library doesn't parse signed key without verification.
         */
        try {
            String withoutSign = removeJwtSign(jwt);
            return Jwts.parser().unsecured().build()
                .parseUnsecuredClaims(withoutSign)
                .getPayload();
        } catch (RuntimeException exception) {
            log.debug("Exception when trying to parse the JWT token %s", jwt);
            return null;
        }
    }

    private static String extractSubFromToken(String token) {
        if (!token.isEmpty()) {
            Claims claims = getJwtClaims(token);
            if (claims != null) {
                return claims.getSubject();
            }
        }
        return "";
    }
}
