/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.config.service.security;

import com.netflix.discovery.DiscoveryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.JwtSecurity;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.util.CacheUtils;

import static org.mockito.Mockito.mock;
import static org.zowe.apiml.zaas.security.service.AuthenticationServiceTest.ZOSMF;

@TestConfiguration
public class MockedAuthenticationServiceContext {
    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public AuthConfigurationProperties getAuthConfigurationProperties() {
        final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();
        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);
        return authConfigurationProperties;
    }

    @Bean
    @Primary
    public JwtSecurity getJwtSecurityInitializer() {
        return mock(JwtSecurity.class);
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return mock(RestTemplate.class);
    }

    @Bean
    public DiscoveryClient getDiscoveryClient() {
        return mock(DiscoveryClient.class);
    }

    @Bean
    public ZosmfService getZosmfService() {
        return mock(ZosmfService.class);
    }

    @Bean
    @Primary
    public AuthenticationService getAuthenticationService(CacheManager cacheManager, CacheUtils cacheUtils) {
        return new AuthenticationService(
            applicationContext, getAuthConfigurationProperties(), getJwtSecurityInitializer(),
            getZosmfService(), getDiscoveryClient(), getRestTemplate(), cacheManager, cacheUtils
        );
    }
}
