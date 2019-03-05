/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.gateway.routing;

import com.broadcom.apiml.library.security.config.SecurityConfigurationProperties;
import com.broadcom.apiml.library.security.token.TokenService;
import com.broadcom.apiml.library.service.security.service.gateway.filters.post.ConvertAuthTokenInUriToCookieFilter;
import com.broadcom.apiml.library.service.security.service.gateway.filters.post.TransformApiDocEndpointsFilter;
import com.broadcom.apiml.library.service.security.service.gateway.filters.pre.LocationFilter;
import com.broadcom.apiml.library.service.security.service.gateway.filters.pre.SlashFilter;
import com.broadcom.apiml.library.service.security.service.gateway.filters.pre.ZosmfFilter;
import com.broadcom.apiml.library.service.security.service.gateway.services.routing.RoutedServicesUser;
import com.broadcom.apiml.library.service.security.service.gateway.ws.WebSocketProxyServerHandler;
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
    public ZosmfFilter zosmfFilter(TokenService tokenService) {
        return new ZosmfFilter(tokenService);
    }

    @Bean
    public TransformApiDocEndpointsFilter transformApiDocEndpointsFilter() {
        return new TransformApiDocEndpointsFilter();
    }

    @Bean
    @Autowired
    public ConvertAuthTokenInUriToCookieFilter convertAuthTokenInUriToCookieFilter(SecurityConfigurationProperties securityConfigurationProperties) {
        return new ConvertAuthTokenInUriToCookieFilter(securityConfigurationProperties);
    }

    @Bean
    @Autowired
    public DiscoveryClientRouteLocator discoveryClientRouteLocator(DiscoveryClient discovery,
                                                                   ZuulProperties zuulProperties,
                                                                   ServiceRouteMapper serviceRouteMapper,
                                                                   WebSocketProxyServerHandler webSocketProxyServerHandler) {
        List<RoutedServicesUser> routedServicesUsers = new ArrayList<>();
        routedServicesUsers.add(locationFilter());
        routedServicesUsers.add(transformApiDocEndpointsFilter());
        routedServicesUsers.add(webSocketProxyServerHandler);

        return new MfaasRouteLocator("", discovery, zuulProperties, serviceRouteMapper, routedServicesUsers);
    }
}
