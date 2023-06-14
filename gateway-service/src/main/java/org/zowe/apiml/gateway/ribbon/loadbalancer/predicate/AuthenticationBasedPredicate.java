/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadbalancer.predicate;

import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import com.netflix.zuul.context.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.gateway.cache.LoadBalancerCache;
import org.zowe.apiml.gateway.ribbon.loadbalancer.LoadBalancingContext;
import org.zowe.apiml.gateway.ribbon.loadbalancer.RequestAwarePredicate;
import org.zowe.apiml.gateway.ribbon.loadbalancer.model.LoadBalancerCacheRecord;
import org.zowe.apiml.gateway.security.service.RequestAuthenticationService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * Based on the authentication information decide which instance should be used.
 * If the user is authenticated and already has routing information stored in cache, use the information.
 * <p>
 * There is also terrible overhead as this happens for all instance ids.
 */
@RequiredArgsConstructor
@Slf4j
public class AuthenticationBasedPredicate extends RequestAwarePredicate {
    private final RequestAuthenticationService authenticationService;
    private final LoadBalancerCache cache;
    private final int expirationTime;

    @Override
    public boolean apply(LoadBalancingContext context, DiscoveryEnabledServer server) {
        RequestContext requestContext = context.getRequestContext();
        String instanceId = context.getInstanceInfo().getInstanceId();
        String serviceId = (String) requestContext.get(SERVICE_ID_KEY);
        if (serviceId == null) {
            // This should never happen
            return true;
        }

        Optional<String> authenticatedUser = authenticationService.getPrincipalFromRequest(requestContext.getRequest());

        if (!authenticatedUser.isPresent()) {
            log.debug("No authentication present on request, not filtering instance: {}", instanceId);
            return true;
        }

        String username = authenticatedUser.get();
        LoadBalancerCacheRecord loadBalancerCacheRecord = cache.retrieve(username, serviceId);
        if (loadBalancerCacheRecord == null || loadBalancerCacheRecord.getInstanceId() == null) {
            log.debug("No preference exists, not filtering instance: {}", instanceId);
            return true;
        }

        if (isTooOld(loadBalancerCacheRecord.getCreationTime())) {
            cache.delete(username, serviceId);
            log.debug("Expired preference exists and was deleted. not filtering instance: {}", instanceId);
            return true;
        }

        return server.getInstanceInfo().getInstanceId().equalsIgnoreCase(loadBalancerCacheRecord.getInstanceId());
    }

    @Override
    public String toString() {
        return "AuthenticationBasedPredicate (USERNAME)";
    }

    private boolean isTooOld(LocalDateTime cachedDate) {
        LocalDateTime now = LocalDateTime.now().minus(expirationTime, ChronoUnit.HOURS);
        return now.isAfter(cachedDate);
    }
}
