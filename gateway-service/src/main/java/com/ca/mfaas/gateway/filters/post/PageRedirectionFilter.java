/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.filters.post;

import com.ca.mfaas.product.routing.ServiceType;
import com.ca.mfaas.product.utils.UrlUtils;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import com.ca.mfaas.product.routing.RoutedServicesUser;
import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.http.HttpHeaders.LOCATION;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

@Slf4j
public class PageRedirectionFilter extends ZuulFilter implements RoutedServicesUser {

    private final DiscoveryClient discovery;
    private final Map<String, RoutedServices> routedServicesMap = new HashMap<>();
    private final ConcurrentHashMap<String, RouteInfo> routeTable = new ConcurrentHashMap<>();
    public PageRedirectionFilter(DiscoveryClient discovery) {
        this.discovery = discovery;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();
        int status = context.getResponseStatusCode();
        return (status > 300 && status < 400);
    }

    @Override
    public String filterType() {
        return POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return SEND_RESPONSE_FILTER_ORDER - 1;
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        Optional<Pair<String, String>> locationHeader = context.getZuulResponseHeaders()
            .stream()
            .filter(stringPair -> LOCATION.equals(stringPair.first()))
            .findFirst();

        if (locationHeader.isPresent()) {
            String location = locationHeader.get().second();
            //find matched url in cache
            Optional<String> serviceUrlWithHostAndPort = foundUrlInTable(location);
            if (serviceUrlWithHostAndPort.isPresent()) {
                transformLocation(locationHeader.get(), routeTable.get(serviceUrlWithHostAndPort.get()));
            } else {
                serviceUrlWithHostAndPort = getMatchedUrlFromDS(location);
                serviceUrlWithHostAndPort.ifPresent(s -> transformLocation(locationHeader.get(), routeTable.get(s)));
            }
        }

        return null;
    }

    private Optional<String> foundUrlInTable(String location) {
        return routeTable.keySet()
            .stream()
            .filter(location::contains)
            .max(Comparator.comparingInt(String::length));
    }

    private Optional<String> getMatchedUrlFromDS(String location) {
        RequestContext context = RequestContext.getCurrentContext();
        String currentServiceId = (String) context.get(SERVICE_ID_KEY);

        String path;
        try {
            path = new URI(location).getPath();
        } catch (URISyntaxException e) {
            log.error("Error creating URI", e);
            return Optional.empty();
        }
        path = UrlUtils.addLastSlash(path);

        //check current service instance
        Optional<String> serviceUrlWithHostAndPort = foundMatchedUrlInService(location, path, currentServiceId);
        if (serviceUrlWithHostAndPort.isPresent()) return serviceUrlWithHostAndPort;

        //iterate through all instances registered in discovery client, check if there is matched url
        List<String> serviceIds = discovery.getServices();
        for (String serviceId : serviceIds) {
            if (!currentServiceId.equals(serviceId)) {
                serviceUrlWithHostAndPort = foundMatchedUrlInService(location, path, serviceId);
                if (serviceUrlWithHostAndPort.isPresent()) {
                    return serviceUrlWithHostAndPort;
                }
            }
        }
        return Optional.empty();
    }

    //Check if path contains service url
    private Optional<String> foundMatchedUrlInService(String location, String path, String serviceId) {
        if (routedServicesMap.containsKey(serviceId)) {
            RoutedService service = routedServicesMap.get(serviceId)
                .getBestMatchingServiceUrl(path, ServiceType.UI);
            if (service != null) {
                String serviceUrl = UrlUtils.removeLastSlash(service.getServiceUrl());
                List<ServiceInstance> serviceInstances = discovery.getInstances(serviceId);
                for (ServiceInstance instance : serviceInstances) {
                    String host = instance.getHost();
                    int port = instance.getPort();
                    String serviceUrlWithHostAndPort = String.join("", host, ":", String.valueOf(port), serviceUrl);
                    if (location.contains(serviceUrlWithHostAndPort)) {
                        //put url to cache
                        addUrlToTable(serviceUrlWithHostAndPort, serviceId, service);
                        return Optional.of(serviceUrlWithHostAndPort);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private void addUrlToTable(String url, String serviceId, RoutedService service) {
        String serviceUrl = service.getServiceUrl();
        if (!serviceUrl.equals("/")) {
            serviceUrl = UrlUtils.removeLastSlash(serviceUrl);
        }
        this.routeTable.put(url, new RouteInfo(serviceId, UrlUtils.addFirstSlash(service.getGatewayUrl()), serviceUrl));
    }

    private void transformLocation(Pair<String, String> locationHeader, RouteInfo route) {
        String gatewayHost = RequestContext.getCurrentContext().getRequest().getLocalName();
        int gatewayPort = RequestContext.getCurrentContext().getRequest().getLocalPort();
        try {
            URI uri = new URI(locationHeader.second());
            String path = uri.getPath();
            if (path != null) {
                if (route.serviceUrl.equals("/")) {
                    path = String.join("", route.prefix, "/", route.serviceId, path);
                } else {
                    path = path.replaceFirst(route.serviceUrl, String.join("", route.prefix, "/", route.serviceId));
                }
            }
            uri = new URI(uri.getScheme(), uri.getUserInfo(), gatewayHost, gatewayPort, path, uri.getQuery(), uri.getFragment());
            locationHeader.setSecond(uri.toString());
        } catch (URISyntaxException e) {
            log.error("Error creating URI", e);
        }
    }

    @Override
    public void addRoutedServices(String serviceId, RoutedServices routedServices) {
        routedServicesMap.put(serviceId, routedServices);
    }

    class RouteInfo {
        private final String serviceId;
        private final String prefix;
        private final String serviceUrl;

        RouteInfo(String serviceId, String prefix, String serviceUrl) {
            this.serviceId = serviceId;
            this.prefix = prefix;
            this.serviceUrl = serviceUrl;
        }
    }
}
