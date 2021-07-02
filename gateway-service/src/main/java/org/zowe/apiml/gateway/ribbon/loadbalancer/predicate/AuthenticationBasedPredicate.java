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
import org.springframework.beans.factory.annotation.Value;
import org.zowe.apiml.gateway.cache.LoadBalancerCache;
import org.zowe.apiml.gateway.ribbon.loadbalancer.LoadBalancingContext;
import org.zowe.apiml.gateway.ribbon.loadbalancer.RequestAwarePredicate;
import org.zowe.apiml.gateway.ribbon.loadbalancer.model.LoadBalancerCacheRecord;
import org.zowe.apiml.gateway.security.service.HttpAuthenticationService;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

/**
 * Based on the authentication information decide which instance should be used.
 * If the user is authenticated and already has routing information stored in cache, use the information.
 * <p>
 * There is also terrible overhead as this happens for all instance ids.
 */
@RequiredArgsConstructor
public class AuthenticationBasedPredicate extends RequestAwarePredicate {
    private final HttpAuthenticationService authenticationService;
    private final LoadBalancerCache cache;

    @Value("${instance.metadata.apiml.lb.cacheRecordExpirationTimeInHours:8}")
    int expirationTime;

    @Override
    public boolean apply(LoadBalancingContext context, DiscoveryEnabledServer server) {
        RequestContext requestContext = context.getRequestContext();
        String serviceId = context.getServiceId();
        if (serviceId == null) {
            // This should never happen
            return true;
        }

        Optional<String> authenticatedUser = authenticationService.getAuthenticatedUser(requestContext.getRequest());

        if (!authenticatedUser.isPresent()) {
            // Allow selection of any instance.
            return true;
        }

        String username = authenticatedUser.get();
        LoadBalancerCacheRecord loadBalancerCacheRecord = cache.retrieve(username, serviceId);
        if (loadBalancerCacheRecord != null && loadBalancerCacheRecord.getInstanceId() != null) {
            long creationTime = loadBalancerCacheRecord.getCreationTime();
            if (isTooOld(creationTime)) {
                cache.delete(username, serviceId);
                return true;
            } else {
                return server.getInstanceInfo().getInstanceId().equalsIgnoreCase(loadBalancerCacheRecord.getInstanceId());
            }
        } else {
            // There is no preference for given user
            return true;
        }
    }

    @Override
    public String toString() {
        return "AuthenticationBasedPredicate (USERNAME)";
    }

    private boolean isTooOld(long cachedDate) {
        Calendar calendar = Calendar.getInstance();
        Date now = new Date();
        calendar.setTime(new Date(cachedDate));
        calendar.add(Calendar.HOUR, expirationTime);
        return now.after(calendar.getTime());
    }
}
