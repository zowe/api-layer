/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.routing;

import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import com.ca.mfaas.product.routing.RoutedServicesUser;
import com.ca.mfaas.product.utils.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
class ApimlRouteLocator extends DiscoveryClientRouteLocator {
    private final DiscoveryClient discovery;
    private final ZuulProperties properties;
    private final List<RoutedServicesUser> routedServicesUsers;

    ApimlRouteLocator(String servletPath, DiscoveryClient discovery, ZuulProperties properties,
                      ServiceRouteMapper serviceRouteMapper, List<RoutedServicesUser> routedServicesUsers) {
        super(servletPath, discovery, properties, serviceRouteMapper, null);
        this.discovery = discovery;
        this.properties = properties;
        this.routedServicesUsers = routedServicesUsers;
    }

    /**
     * Suppressing warnings instead of resolving them to match the original class
     * DiscoveryClientRouteLocator as much as possible
     */
    @Override
    @SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S1075", "squid:S3776"})
    protected LinkedHashMap<String, ZuulProperties.ZuulRoute> locateRoutes() {
        LinkedHashMap<String, ZuulProperties.ZuulRoute> routesMap = new LinkedHashMap<>(super.locateRoutes());
        if (this.discovery != null) {
            Map<String, ZuulProperties.ZuulRoute> staticServices = new LinkedHashMap<>();
            for (ZuulProperties.ZuulRoute route : routesMap.values()) {
                String serviceId = route.getServiceId();
                if (serviceId == null) {
                    serviceId = route.getId();
                }
                if (serviceId != null) {
                    staticServices.put(serviceId, route);
                }
            }

            // Add routes for discovered services and itself by default
            List<String> services = this.discovery.getServices();

            String[] ignored = this.properties.getIgnoredServices()
                .toArray(new String[0]);
            Set<String> removedRoutes = new HashSet<>();
            for (String serviceId : services) {
                // Ignore specifically ignored services and those that were manually
                // configured
                RoutedServices routedServices = new RoutedServices();
                List<ServiceInstance> serviceInstances = this.discovery.getInstances(serviceId);
                if (serviceInstances == null || serviceInstances.isEmpty()) {
                    log.error("No instance of the service {} found. Routing will not be available.", serviceId);
                    return null;
                }

                List<String> keys = createRouteKeys(serviceInstances, routedServices, serviceId);
                if (keys.isEmpty()) {
                    keys.add("/" + mapRouteToService(serviceId) + "/**");
                }

                for (RoutedServicesUser routedServicesUser : routedServicesUsers) {
                    routedServicesUser.addRoutedServices(serviceId, routedServices);
                }

                if (staticServices.containsKey(serviceId)
                    && staticServices.get(serviceId).getUrl() == null) {
                    // Explicitly configured with no URL, they are the default routes from the parent
                    // We need to remove them
                    ZuulProperties.ZuulRoute staticRoute = staticServices.get(serviceId);
                    routesMap.remove(staticRoute.getPath());
                    removedRoutes.add(staticRoute.getPath());
                }

                for (String key : keys) {
                    if (!PatternMatchUtils.simpleMatch(ignored, serviceId)
                        && !routesMap.containsKey(key) && !removedRoutes.contains(key)) {
                        // Not ignored
                        routesMap.put(key, new ZuulProperties.ZuulRoute(key, serviceId));
                    }
                }
            }
        }

        LinkedHashMap<String, ZuulProperties.ZuulRoute> values = new LinkedHashMap<>();
        for (Map.Entry<String, ZuulProperties.ZuulRoute> entry : routesMap.entrySet()) {
            String path = entry.getKey();
            // Prepend with slash if not already present.
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (StringUtils.hasText(this.properties.getPrefix())) {
                path = this.properties.getPrefix() + path;
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
            }
            values.put(path, entry.getValue());
        }

        return values;
    }

    /**
     * Parse route keys from the metadata and populate service routes
     *
     * @param serviceInstance the list of service instances
     * @param routes          the service routes
     * @param serviceId       the service id
     * @return the list of route keys
     */
    @SuppressWarnings("squid:S3776") // Suppress complexity warning
    private List<String> createRouteKeys(List<ServiceInstance> serviceInstance,
                                         RoutedServices routes, String serviceId) {
        List<String> keys = new ArrayList<>();

        for (ServiceInstance instance : serviceInstance) {
            Map<String, String> metadataMap = new TreeMap<>(instance.getMetadata());
            Map<String, String> routeMap = new HashMap<>();

            for (Map.Entry<String, String> metadata : metadataMap.entrySet()) {
                String[] url = metadata.getKey().split("\\.");
                if (url.length == 3 && url[0].equals(RoutedServices.ROUTED_SERVICES_PARAMETER)) {

                    if (url[2].equals(RoutedServices.GATEWAY_URL_PARAMETER)) {
                        String gatewayURL = UrlUtils.removeFirstAndLastSlash(metadata.getValue());
                        routeMap.put(url[1], gatewayURL);
                        keys.add("/" + gatewayURL + "/" + mapRouteToService(serviceId) + "/**");
                    }

                    if (url[2].equals(RoutedServices.SERVICE_URL_PARAMETER) && routeMap.containsKey(url[1])) {
                        String serviceURL = UrlUtils.addFirstSlash(metadata.getValue());
                        routes.addRoutedService(new RoutedService(url[1], routeMap.get(url[1]), serviceURL));
                        routeMap.remove(url[1]);
                    }
                }
            }
        }
        return keys;
    }
}
