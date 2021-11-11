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
import lombok.RequiredArgsConstructor;
import org.zowe.apiml.gateway.cache.LoadBalancerCache;
import org.zowe.apiml.gateway.ribbon.RequestContextUtils;
import org.zowe.apiml.gateway.ribbon.loadbalancer.model.LoadBalancerCacheRecord;
import org.zowe.apiml.gateway.security.service.RequestAuthenticationService;

import java.util.Map;
import java.util.Optional;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * This post filter allows, in case of sticky session, to store the instance selected by the RequestHeaderPredicate to the cache.
 * The filter checks whether the service requires a sticky session. It also checks whether the user is authenticated and
 * if the user is authenticated and there is no instance in the cache stores the selected instance in the cache.
 */
@RequiredArgsConstructor
public class PostStoreLoadBalancerCacheFilter extends ZuulFilter {

    private final RequestAuthenticationService authenticationService;
    private final LoadBalancerCache loadBalancerCache;

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
    @SuppressWarnings("squid:S3516") // We always have to return null
    public Object run() {
        Optional<InstanceInfo> instance = RequestContextUtils.getInstanceInfo();
        if (!instance.isPresent()) {
            return null;
        }

        InstanceInfo selectedInstance = instance.get();
        Map<String, String> metadata = selectedInstance.getMetadata();
        if (metadata == null) {
            return null;
        }
        String lbType = metadata.get("apiml.lb.type");
        if (lbType == null
            || !lbType.equals("authentication")
            || selectedInstance.getInstanceId() == null
        ) {
            return null;
        }

        RequestContext context = RequestContext.getCurrentContext();
        String currentServiceId = (String) context.get(SERVICE_ID_KEY);
        Optional<String> principal = authenticationService.getPrincipalFromRequest(context.getRequest());
        if (principal.isPresent()) {
            // Dont store instance info when there is exception in request processing. This means failed request.
            if (context.get("throwable") != null) {
                return null;
            }

            // Also take into account whether it's for the first time and what do we know here.
            LoadBalancerCacheRecord loadBalancerCacheRecord = new LoadBalancerCacheRecord(instance.get().getInstanceId());
            loadBalancerCache.store(principal.get(), currentServiceId, loadBalancerCacheRecord);
        }

        return null;
    }
}
