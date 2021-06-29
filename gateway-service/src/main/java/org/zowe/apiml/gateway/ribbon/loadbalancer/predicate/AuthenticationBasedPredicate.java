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
import org.zowe.apiml.gateway.cache.LoadBalancerCache;
import org.zowe.apiml.gateway.ribbon.loadbalancer.LoadBalancingContext;
import org.zowe.apiml.gateway.ribbon.loadbalancer.RequestAwarePredicate;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.util.Optional;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * Based on the authentication information decide which instance should be used.
 * If the user is authenticated and already has routing information stored in cache, use the information.
 *
 * TODO: Evict the information based on the configuration.
 * There is also terrible overhead as this happens for all instance ids.
 */
@RequiredArgsConstructor
public class AuthenticationBasedPredicate extends RequestAwarePredicate {
    private final AuthenticationService authenticationService;
    private final LoadBalancerCache cache;

    @Override
    public boolean apply(LoadBalancingContext context, DiscoveryEnabledServer server) {
        RequestContext requestContext = context.getRequestContext();
        String serviceId = (String) requestContext.get(SERVICE_ID_KEY);
        if (serviceId == null) {
            // This should never happen
            return true;
        }

        Optional<String> authenticatedUser = getUsername(requestContext);

        if (!authenticatedUser.isPresent()) {
            // Allow selection of any instance.
            return true;
        }

        String username = authenticatedUser.get();
        String instanceId = cache.retrieve(username, serviceId);
        if(instanceId != null) {
            return server.getInstanceInfo().getInstanceId().equalsIgnoreCase(instanceId);
        } else {
            // There is no preference for given user
            return true;
        }
    }

    private Optional<String> getUsername(RequestContext context) {
        String jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest()).orElse(null);
        if (jwtToken != null) {
            TokenAuthentication authentication = authenticationService.validateJwtToken(jwtToken);
            if (authentication.isAuthenticated()) {
                return Optional.of(authentication.getPrincipal());
            }
        }

        return Optional.empty();
    }

    @Override
    public String toString() {
        return "AuthenticationBasedPredicate (USERNAME)";
    }
}
