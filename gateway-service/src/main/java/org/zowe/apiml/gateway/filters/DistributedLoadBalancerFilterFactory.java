/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.caching.LoadBalancerCache;
import org.zowe.apiml.gateway.caching.LoadBalancerCache.LoadBalancerCacheRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A custom gateway filter factory that integrates with Eureka and uses a load balancer cache to manage sticky sessions.
 */
@Component
@Slf4j
public class DistributedLoadBalancerFilterFactory extends AbstractGatewayFilterFactory<DistributedLoadBalancerFilterFactory.Config> {
    private final LoadBalancerCache cache;
    private final EurekaClient eurekaClient;
    private final static String APIML_TOKEN = "apimlAuthenticationToken";
    private static final String HEADER_NONE_SIGNATURE = Base64.getEncoder().encodeToString("""
        {"typ":"JWT","alg":"none"}""".getBytes(StandardCharsets.UTF_8));
    public DistributedLoadBalancerFilterFactory(
        EurekaClient eurekaClient,
        LoadBalancerCache loadBalancerCache) {
        super(Config.class);
        this.eurekaClient = eurekaClient;
        this.cache = loadBalancerCache;
    }

    /**
     * Store the user and service information into the cache,
     * in case the service metadata load balancer type is set to "authentication".
     *
     * @param config the filter configuration
     * @return the gateway filter
     */
    @Override
    public GatewayFilter apply(DistributedLoadBalancerFilterFactory.Config config) {
        return (exchange, chain) -> Mono.fromCallable(() -> eurekaClient.getInstancesById(config.instanceId))
            .map(instances -> {
                if (shouldIgnore(instances)) {
                    return chain.filter(exchange);
                }
                String sub = extractSubFromToken(exchange.getRequest());
                    if (sub.isEmpty()) {
                        log.debug("No authentication present on request, the distributed load balancer will not be performed for the service {}", config.getServiceId());
                        return Flux.empty();
                    } else {
                        exchange.getAttributes().put("Token-Subject", sub);
                        LoadBalancerCacheRecord loadBalancerCacheRecord = new LoadBalancerCacheRecord(config.getInstanceId());
                        return cache.store(sub, config.getServiceId(), loadBalancerCacheRecord);
                    }
            }).then();
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
            throw new JwtException(String.format("Exception when trying to parse the JWT token %s", jwt));
        }
    }

    private String extractSubFromToken(ServerHttpRequest request) {
        if (request != null) {
            String token = Objects.requireNonNull(request.getCookies().getFirst(APIML_TOKEN)).getValue();
            if (!token.isEmpty()) {
                Claims claims = getJwtClaims(token);
                return claims.getSubject();
            }
        }
        return null;
    }

    boolean shouldIgnore(List<?> instances) {
        return instances.isEmpty() || !(instances.get(0) instanceof InstanceInfo instanceInfo) || !lbTypeIsAuthentication(instanceInfo);
    }

    /**
     * Checks if the load balancer type for the instance defined in the metadata is "authentication".
     *
     * @param selectedInstance the selected instance
     * @return true if the load balancer type is "authentication", false otherwise
     */
    private boolean lbTypeIsAuthentication(InstanceInfo selectedInstance) {
        Map<String, String> metadata = selectedInstance.getMetadata();
        if (metadata != null) {
            String lbType = metadata.get("apiml.lb.type");
            return lbType != null && lbType.equals("authentication");
        }
        return false;
    }

    @Data
    public static class Config {

        private String serviceId;
        private String instanceId;
    }
}
