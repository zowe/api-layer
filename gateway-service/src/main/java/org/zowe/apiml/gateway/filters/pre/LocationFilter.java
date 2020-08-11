/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.filters.pre;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.RoutedServicesUser;
import org.zowe.apiml.util.UrlUtils;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * Must be run after PreDecorationFilter. This will set Proxy, ServiceId and other variables in RequestContext
 */
@Slf4j
public class LocationFilter extends ZuulFilter implements RoutedServicesUser {

    private final Map<String, RoutedServices> routedServicesMap = new HashMap<>();

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 3;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();

        final String serviceId = (String) context.get(SERVICE_ID_KEY);
        final String proxy = UrlUtils.removeFirstAndLastSlash((String) context.get(PROXY_KEY));
        final String requestPath = UrlUtils.addFirstSlash((String) context.get(REQUEST_URI_KEY));

        if (isRequestThatCanBeProcessed(serviceId, proxy, requestPath)) {
            RoutedServices routedServices = routedServicesMap.get(serviceId);

            if (routedServices != null) {
                @SuppressWarnings("squid:S2259")
                int i = proxy.lastIndexOf('/');

                if (i > 0) {
                    String originalPath = normalizeOriginalPath(getService(routedServices, proxy).getServiceUrl());
                    context.set(REQUEST_URI_KEY, originalPath + requestPath);
                    log.debug("Routing: The request was routed to {}", originalPath + requestPath);
                }
            } else {
                log.trace("Routing: No routing metadata for service {} found.", serviceId);
            }
        } else {
            log.trace("Routing: Incorrect serviceId {}, proxy {} or requestPath {}.", serviceId, proxy, requestPath);
        }

        return null;
    }

    private RoutedService getService(RoutedServices routedServices, String proxy) {
        // Try to find service by route using old API path format /{typeOfService}/{version}/{serviceId} //NOSONAR
        String route = proxy.substring(0, proxy.lastIndexOf('/'));
        RoutedService service = routedServices.findServiceByGatewayUrl(route);
        if (service == null) {
            // If not found, try by route using new API path format /{serviceId}/{typeOfService}/{version} //NOSONAR
            route = proxy.substring(proxy.indexOf('/') + 1);
            service = routedServices.findServiceByGatewayUrl(route);
        }
        return service;
    }

    public void addRoutedServices(String serviceId, RoutedServices routedServices) {
        routedServicesMap.put(serviceId, routedServices);
    }

    private boolean isRequestThatCanBeProcessed(String serviceId, String proxy, String requestPath) {
        return !(serviceId == null || proxy == null || requestPath == null);
    }

    private String normalizeOriginalPath(String originalPath) {
        if (originalPath == null) {
            return "";
        } else {
            return originalPath;
        }
    }
}
