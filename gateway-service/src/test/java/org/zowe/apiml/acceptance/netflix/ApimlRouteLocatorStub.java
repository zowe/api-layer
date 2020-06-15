/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.acceptance.netflix;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.zowe.apiml.gateway.routing.ApimlRouteLocator;
import org.zowe.apiml.product.routing.RoutedServicesUser;

import java.util.LinkedHashMap;
import java.util.List;

public class ApimlRouteLocatorStub extends ApimlRouteLocator {
    private ApplicationRegistry applicationRegistry;

    public ApimlRouteLocatorStub(String servletPath, DiscoveryClient discovery, ZuulProperties properties, ServiceRouteMapper serviceRouteMapper, List<RoutedServicesUser> routedServicesUsers, ApplicationRegistry applicationRegistry) {
        super(servletPath, discovery, properties, serviceRouteMapper, routedServicesUsers);

        this.applicationRegistry = applicationRegistry;
        applicationRegistry.setRoutedServices(routedServicesUsers);
    }

    @Override
    protected LinkedHashMap<String, ZuulProperties.ZuulRoute> locateRoutes() {
        return applicationRegistry.getRoutes();
    }

}
