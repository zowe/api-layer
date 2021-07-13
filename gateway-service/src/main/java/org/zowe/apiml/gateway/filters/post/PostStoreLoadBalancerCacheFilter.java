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
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.gateway.cache.LoadBalancerCache;
import org.zowe.apiml.gateway.ribbon.RequestContextUtils;
import org.zowe.apiml.gateway.ribbon.loadbalancer.model.LoadBalancerCacheRecord;
import org.zowe.apiml.gateway.security.service.HttpAuthenticationService;

import java.util.Map;
import java.util.Optional;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * This post filter allows, in case of sticky session, to store the instance selected by the RequestHeaderPredicate to the cache.
 * The filter checks whether the service requires a sticky session. It also checks whether the user is authenticated and
 * if the user is authenticated and there is no instance in the cache stores the selected instance in the cache.
 */
@RequiredArgsConstructor
@Slf4j
public class PostStoreLoadBalancerCacheFilter extends ZuulFilter {

    private final HttpAuthenticationService authenticationService;
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
        log.info("PostStoreLoadBalancerCacheFilter#run");
        Optional<InstanceInfo> instance = RequestContextUtils.getInstanceInfo();
        if (!instance.isPresent()) {
            log.info("PostStoreLoadBalancerCacheFilter#run No instance is present");
            return null;
        }

        InstanceInfo selectedInstance = instance.get();
        Map<String, String> metadata = selectedInstance.getMetadata();
        if (metadata == null) {
            log.info("PostStoreLoadBalancerCacheFilter#run No metadata for the instance");
            return null;
        }
        String lbType = metadata.get("apiml.lb.type");
        log.info("PostStoreLoadBalancerCacheFilter#run Load Balancing Type: {}", lbType);
        if (lbType == null
            || !lbType.equals("authentication")
            || selectedInstance.getInstanceId() == null
        ) {
            return null;
        }

        RequestContext context = RequestContext.getCurrentContext();
        String currentServiceId = (String) context.get(SERVICE_ID_KEY);
        log.info("PostStoreLoadBalancerCacheFilter#run Current Service: {}", currentServiceId);
        Optional<String> authenticatedUser = authenticationService.getAuthenticatedUser(context.getRequest());
        log.info("PostStoreLoadBalancerCacheFilter#run Authenticated User: {}", authenticatedUser.get());
        if (authenticatedUser.isPresent() && !instanceIsCached(authenticatedUser.get(), currentServiceId)) {
            // Dont store instance info when failed.
            if (context.get("throwable") != null) {
                log.info("PostStoreLoadBalancerCacheFilter#run Throwable was part of the response");
                return null;
            }

            // Also take into account whether it's for the first time and what do we know here.
            LoadBalancerCacheRecord loadBalancerCacheRecord = new LoadBalancerCacheRecord(instance.get().getInstanceId());
            log.info("PostStoreLoadBalancerCacheFilter#run Store to the cache");
            loadBalancerCache.store(authenticatedUser.get(), currentServiceId, loadBalancerCacheRecord);
        }

        return null;
    }

    private boolean instanceIsCached(String user, String service) {
        return loadBalancerCache.retrieve(user, service) != null;
    }
}
