/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.post;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.zowe.apiml.gateway.cache.LoadBalancerCache;
import org.zowe.apiml.gateway.ribbon.RequestContextUtils;
import org.zowe.apiml.gateway.security.service.AuthenticationService;

import java.util.*;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * This post filter allows, in case of sticky session, to store the instance selected by the RequestHeaderPredicate to the cache.
 * The filter checks whether the service requires a sticky session. It also checks whether the user is authenticated and
 * if the user is authenticated and there is no instance in the cache stores the selected instance in the cache.
 */
@Getter
@RequiredArgsConstructor
public class PostStoreLoadBalancerCacheFilter extends ZuulFilter {

    @Autowired
    private AuthenticationService authenticationService;

    private final LoadBalancerCache loadBalancerCache = new LoadBalancerCache();

    @Override
    public String filterType() {
        return POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return SEND_RESPONSE_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        String currentServiceId = (String) context.get(SERVICE_ID_KEY);
        Optional<String> jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest());
        if (jwtToken.isPresent()) {

            Optional<InstanceInfo> instance = RequestContextUtils.getInstanceInfo();
            if (instance.isPresent()) {
                if (checkIfInstanceIsCached(user, currentServiceId)) {
                    loadBalancerCache.store(user, currentServiceId, instance.get().getInstanceId());
                }
            }
        }
        return null;
    }

    private boolean checkIfInstanceIsCached(String user, String service) {
        return loadBalancerCache.retrieve(user, service) == null || loadBalancerCache.retrieve(user, service).isEmpty();
    }
}
