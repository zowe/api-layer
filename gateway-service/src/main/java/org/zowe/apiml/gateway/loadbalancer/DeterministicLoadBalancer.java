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

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.ExpiredJWTException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.zowe.apiml.gateway.caching.LoadBalancerCache;
import org.zowe.apiml.gateway.caching.LoadBalancerCache.LoadBalancerCacheRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.zowe.apiml.constants.ApimlConstants.X_INSTANCEID;
import static reactor.core.publisher.Flux.just;

/**
 * A sticky session load balancer that ensures requests from the same user are routed to the same service instance.
 */
@Slf4j
public class DeterministicLoadBalancer extends SameInstancePreferenceServiceInstanceListSupplier {

    public static final String HEADER_PREFIX = "Bearer ";
    private static final String HEADER_NONE_SIGNATURE = Base64.getEncoder().encodeToString("{\"typ\":\"JWT\",\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));

    private final LoadBalancerCache cache;
    private final int expirationTime;
    private final Clock clock;

    public DeterministicLoadBalancer(ServiceInstanceListSupplier delegate,
                                     ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory,
                                     LoadBalancerCache cache,
                                     int expirationTime,
                                     Clock clock) {
        super(delegate, loadBalancerClientFactory);
        this.cache = cache;
        this.expirationTime = expirationTime;
        this.clock = clock;
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

        var requestContext = request.getContext();
        var instanceId = getInstanceId(requestContext);
        if (instanceId != null) {
            // if instanceId is set in headers use it
            try {
                return delegate.get(request)
                    .map(serviceInstances -> checkInstanceIdHeader(instanceId, serviceInstances));
            } catch (ResponseStatusException ex) {
                return Flux.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Service instance not found for the provided instance ID"));
            }
        }

        var userId = getSub(requestContext);
        if (userId == null) {
            // if no userId is available return all
            log.debug("No authentication present on request, not filtering the service: {}", serviceId);
            return delegate.get(request);
        }

        return delegate.get(request)
            .flatMap(serviceInstances -> {
                if (serviceInstances.isEmpty()) {
                    // no instances available - just return
                    log.debug("No services selected");
                    return Flux.just(serviceInstances);
                }

                boolean stickySession = lbTypeIsAuthentication(serviceInstances.iterator().next());
                if (!stickySession) {
                    // service does not support sticky session by userId, just return
                    log.debug("Service {} does not support sticky session", serviceId);
                    return Flux.just(serviceInstances);
                }

                log.debug("Obtain service instances for {} from the cache", serviceId);
                return cache.retrieve(userId, serviceId)
                    .onErrorResume(t -> Mono.empty())
                    .flatMapMany(cacheRecord -> filterInstances(userId, serviceId, cacheRecord, serviceInstances))
                    .switchIfEmpty(Flux.just(serviceInstances));
            })
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

    private String getSub(Object requestContext) {
        if (requestContext instanceof RequestDataContext ctx) {
            var token = Optional.ofNullable(getTokenFromCookie(ctx))
                                .orElseGet(() -> getTokenFromHeader(ctx));
            return extractSubFromToken(token);
        }
        return null;
    }

    private String getTokenFromCookie(RequestDataContext ctx) {
        return ctx.getClientRequest().getCookies().getFirst("apimlAuthenticationToken");
    }

    private String getTokenFromHeader(RequestDataContext ctx) {
        var authHeaderValue = ctx.getClientRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (Strings.CS.startsWith(authHeaderValue, HEADER_PREFIX)) {
            return authHeaderValue.substring(HEADER_PREFIX.length());
        }
        return null;
    }

    /**
     * Filters the list of service instances to include only those with the specified instance ID.
     * Optional operation, it verifies if the conditions are met to actually filter the list of instances.
     *
     * @param user             The user
     * @param serviceId        The serviceId
     * @param cacheRecord           the cache record
     * @param serviceInstances the list of service instances to filter
     * @return the filtered list of service instances
     */
    private Flux<List<ServiceInstance>> filterInstances(
        String user,
        String serviceId,
        LoadBalancerCacheRecord cacheRecord,
        List<ServiceInstance> serviceInstances
    ) {
        if (isNotBlank(cacheRecord.getInstanceId())) {
            if (isTooOld(cacheRecord.getCreationTime())) {
                return cache.delete(user, serviceId)
                    .thenMany(chooseOne(user, serviceInstances));
            }
            return chooseOne(cacheRecord.getInstanceId(), user, serviceInstances);
        }

        return chooseOne(user, serviceInstances);
    }

    /**
     * Retrieves the 'X-InstanceId' attribute from the request context.
     *
     * @param requestContext the request context
     * @return the instance ID, or null if not found
     */
    private String getInstanceId(Object requestContext) {
        if (requestContext instanceof RequestDataContext ctx) {
            return getInstanceFromHeader(ctx);
        }
        return null;
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
     * Applies soft preference for the requested instance ID: the matching instance is placed
     * first in the list with all other instances following as fallback. Throws 404 only if the
     * requested instance ID does not exist anywhere in the service instances list.
     *
     * @param instanceId       ID of the service instance
     * @param serviceInstances the list of service instances
     * @return ordered list with matched instance first, all others following
     */
    private List<ServiceInstance> checkInstanceIdHeader(String instanceId, List<ServiceInstance> serviceInstances) {
        if (instanceId != null) {
            Optional<ServiceInstance> matchedInstance = serviceInstances.stream()
                .filter(instance -> instanceId.equals(instance.getInstanceId()))
                .findFirst();
            if (matchedInstance.isPresent()) {
                List<ServiceInstance> orderedList = new ArrayList<>();
                orderedList.add(matchedInstance.get());
                for (ServiceInstance instance : serviceInstances) {
                    if (!instance.getInstanceId().equals(instanceId)) {
                        orderedList.add(instance);
                    }
                }
                return orderedList;
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Service instance not found for the provided instance ID");
        }
        return serviceInstances;
    }


    /**
     * Returns the ordered list of service instances with the preferred instance first.
     * If the preferred instance is found (by instanceId), it is placed at the head of the list
     * with all other instances following as fallback. The cache always stores the preferred
     * instance for sticky affinity. If no preference exists, the first instance from the list
     * becomes the new preference.
     *
     * @param instanceId       The preferred instanceId
     * @param user             The user
     * @param serviceInstances The default serviceInstances available
     * @return Flux with the ordered list: preferred instance first, all others following
     */
    private Flux<List<ServiceInstance>> chooseOne(String instanceId, String user, List<ServiceInstance> serviceInstances) {
        ServiceInstance chosenInstance;
        if (instanceId != null) {
            chosenInstance = serviceInstances.stream()
                .filter(instance -> instanceId.equals(instance.getInstanceId()))
                .findAny()
                .orElse(serviceInstances.get(0));
        } else {
            chosenInstance = serviceInstances.get(0);
        }
        List<ServiceInstance> orderedList = new ArrayList<>(serviceInstances.size());
        orderedList.add(chosenInstance);
        for (ServiceInstance instance : serviceInstances) {
            if (!instance.getInstanceId().equals(chosenInstance.getInstanceId())) {
                orderedList.add(instance);
            }
        }
        return cache.store(user, chosenInstance.getServiceId(), new LoadBalancerCacheRecord(chosenInstance.getInstanceId()))
            .thenMany(just(orderedList));
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

    private boolean lbTypeIsAuthentication(ServiceInstance instance) {
        Map<String, String> metadata = instance.getMetadata();
        if (metadata != null) {
            String lbType = metadata.get("apiml.lb.type");
            return lbType != null && lbType.equals("authentication");
        }
        return false;
    }

    private String removeJwtSign(String jwtToken) throws BadJWTException {
        if (jwtToken == null) return null;

        int firstDot = jwtToken.indexOf('.');
        int lastDot = jwtToken.lastIndexOf('.');
        if ((firstDot < 0) || (firstDot >= lastDot)) {
            throw new BadJWTException("Invalid JWT format");
        }

        return HEADER_NONE_SIGNATURE + jwtToken.substring(firstDot, lastDot + 1);
    }

    private JWTClaimsSet getJwtClaims(String jwt) {
        /*
         * Removes signature, because we don't have key to verify z/OS tokens, and we just need to read claim.
         * Verification is done by SAF itself. JWT library doesn't parse signed key without verification.
         */
        try {
            var jwtWithoutSignature = removeJwtSign(jwt);

            var claims = JWTParser.parse(jwtWithoutSignature)
                .getJWTClaimsSet();
            if (claims.getExpirationTime().toInstant().isBefore(clock.instant())) {
                throw new ExpiredJWTException("JWT Token is expired");
            }
            return claims;
        } catch (RuntimeException | ParseException | BadJWTException exception) {
            log.debug("Exception when trying to parse the JWT token {}: {}", jwt, exception.getMessage());
            return null; // NOSONAR
        }
    }

    private String extractSubFromToken(String token) {
        if (StringUtils.isNotEmpty(token)) {
            var claims = getJwtClaims(token);
            if (claims != null) {
                return claims.getSubject();
            }
        }
        return null;
    }

}
