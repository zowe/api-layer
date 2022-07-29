/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.routing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.gateway.cache.LoadBalancerCache;
import org.zowe.apiml.gateway.filters.post.*;
import org.zowe.apiml.gateway.filters.pre.*;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.RequestAuthenticationService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.routing.transform.TransformService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

@Configuration
public class FilterConfig {
    @Bean
    public LocationFilter locationFilter() {
        return new LocationFilter();
    }

    @Bean
    public EncodedCharactersFilter encodedCharactersFilter(DiscoveryClient discovery,
                                                           MessageService messageService) {
        return new EncodedCharactersFilter(discovery, messageService);
    }

    @Bean
    public SlashFilter slashFilter() {
        return new SlashFilter();
    }

    @Bean
    public ServiceAuthenticationFilter serviceAuthenticationFilter() {
        return new ServiceAuthenticationFilter();
    }

    @Bean
    public ServiceNotFoundFilter serviceNotFoundFilter() {
        return new ServiceNotFoundFilter();
    }

    @Bean
    public PageRedirectionFilter pageRedirectionFilter(DiscoveryClient discovery,
                                                       TransformService transformService) {
        return new PageRedirectionFilter(discovery, transformService);
    }

    @Bean
    @Autowired
    public ConvertAuthTokenInUriToCookieFilter convertAuthTokenInUriToCookieFilter(AuthConfigurationProperties authConfigurationProperties) {
        return new ConvertAuthTokenInUriToCookieFilter(authConfigurationProperties);
    }

    @Bean
    @ConditionalOnProperty(name = "apiml.routing.instanceIdHeader", havingValue = "true")
    public RoutedInstanceIdFilter routedServerFilter() {
        return new RoutedInstanceIdFilter();
    }

    @Bean
    public PostStoreLoadBalancerCacheFilter postStoreLoadBalancerCacheFilter(AuthenticationService authenticationService,
                                                                             LoadBalancerCache cache) {
        return new PostStoreLoadBalancerCacheFilter(new RequestAuthenticationService(authenticationService), cache);
    }
}
