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

import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.ServiceAuthenticationServiceImpl;
import org.zowe.apiml.zaas.security.service.schema.AuthenticationSchemeFactory;
import org.zowe.apiml.zaas.security.service.schema.ServiceAuthenticationService;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.util.CacheUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@TestConfiguration
public class MockedSecurityContext {

    @Bean
    public EurekaClient getDiscoveryClient() {
        return mock(EurekaClient.class);
    }

    @Bean
    public EurekaMetadataParser getEurekaMetadataParser() {
        return spy(new EurekaMetadataParser());
    }

    @Bean
    public AuthenticationSchemeFactory getAuthenticationSchemeFactory() {
        return mock(AuthenticationSchemeFactory.class);
    }

    @Bean
    public AuthenticationService getAuthenticationService() {
        return mock(AuthenticationService.class);
    }

    @Bean
    public AuthSourceService getAuthSourceService() {
        return mock(AuthSourceService.class);
    }

    @Bean
    public ServiceAuthenticationService getServiceAuthenticationService(ApplicationContext applicationContext, @Autowired CacheManager cacheManager, CacheUtils cacheUtils) {
        return new ServiceAuthenticationServiceImpl(
                applicationContext,
                getDiscoveryClient(),
                getEurekaMetadataParser(),
                getAuthenticationSchemeFactory(),
                cacheManager,
                cacheUtils
        );
    }

}
