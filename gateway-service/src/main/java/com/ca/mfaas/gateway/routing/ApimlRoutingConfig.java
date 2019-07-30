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

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.mfaas.gateway.filters.post.ConvertAuthTokenInUriToCookieFilter;
import com.ca.mfaas.gateway.filters.post.PageRedirectionFilter;
import com.ca.mfaas.gateway.filters.pre.LocationFilter;
import com.ca.mfaas.gateway.filters.pre.SlashFilter;
import com.ca.mfaas.gateway.filters.pre.ZosmfFilter;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.ca.mfaas.gateway.ws.WebSocketProxyServerHandler;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.ca.mfaas.product.routing.RoutedServicesUser;
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
public class ApimlRoutingConfig {

    @Bean
    public LocationFilter locationFilter() {
        return new LocationFilter();
    }

    @Bean
    public SlashFilter slashFilter() {
        return new SlashFilter();
    }

    @Bean
    public ZosmfFilter zosmfFilter(AuthenticationService authenticationService) {
        return new ZosmfFilter(authenticationService);
    }

    @Bean
    @Autowired
    public PageRedirectionFilter pageRedirectionFilter(DiscoveryClient discovery,
                                                       GatewayConfigProperties gatewayConfigProperties) {
        return new PageRedirectionFilter(discovery, gatewayConfigProperties);
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
                                                                   WebSocketProxyServerHandler webSocketProxyServerHandler,
                                                                   PageRedirectionFilter pageRedirectionFilter) {
        List<RoutedServicesUser> routedServicesUsers = new ArrayList<>();
        routedServicesUsers.add(locationFilter());
        routedServicesUsers.add(webSocketProxyServerHandler);
        routedServicesUsers.add(pageRedirectionFilter);

        return new ApimlRouteLocator("", discovery, zuulProperties, serviceRouteMapper, routedServicesUsers);
    }
}
