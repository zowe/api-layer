/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.config.routing;

import com.ca.mfaas.enable.services.MfaasServiceLocator;
import com.ca.mfaas.gateway.filters.post.TransformApiDocEndpointsFilter;
import com.ca.mfaas.gateway.filters.pre.LocationFilter;
import com.ca.mfaas.gateway.filters.pre.SlashFilter;
import com.ca.mfaas.gateway.services.routing.RoutedServicesUser;
import com.ca.mfaas.gateway.ws.WebSocketProxyServerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MfaasRoutingConfig {

    @Bean
    public LocationFilter locationFilter() {
        return new LocationFilter();
    }

    @Bean
    public SlashFilter slashFilter() {
        return new SlashFilter();
    }

    @Bean
    public TransformApiDocEndpointsFilter transformApiDocEndpointsFilter() {
        return new TransformApiDocEndpointsFilter();
    }

    @Bean
    @Autowired
    public DiscoveryClientRouteLocator discoveryClientRouteLocator(DiscoveryClient discovery,
                                                                   ZuulProperties zuulProperties,
                                                                   ServiceRouteMapper serviceRouteMapper,
                                                                   WebSocketProxyServerHandler webSocketProxyServerHandler,
                                                                   MfaasServiceLocator mfaasServiceLocator) {
        List<RoutedServicesUser> routedServicesUsers = new ArrayList<>();
        routedServicesUsers.add(locationFilter());
        routedServicesUsers.add(transformApiDocEndpointsFilter());
        routedServicesUsers.add(webSocketProxyServerHandler);

        return new MfaasRouteLocator("", discovery, zuulProperties, serviceRouteMapper, routedServicesUsers, mfaasServiceLocator);
    }
}
