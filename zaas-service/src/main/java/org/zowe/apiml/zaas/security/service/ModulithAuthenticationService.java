/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.registry.SelfRegistration;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.util.CacheUtils;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;

@Slf4j
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ConditionalOnBean(name = "modulithConfig")
public class ModulithAuthenticationService extends AuthenticationService {

    public ModulithAuthenticationService(ApplicationContext applicationContext,
            AuthConfigurationProperties authConfigurationProperties, JwtSecurity jwtSecurityInitializer,
            ZosmfService zosmfService, DiscoveryClient discoveryClient, SelfRegistration selfRegistration,
            RestTemplate restTemplate, CacheManager cacheManager, CacheUtils cacheUtils) {
        super(applicationContext, authConfigurationProperties, jwtSecurityInitializer, zosmfService, discoveryClient,
                selfRegistration, restTemplate, cacheManager, cacheUtils);
    }

    /**
     * In the modulith the authentication endpoints are served by the Gateway, under its own path, rather than
     * by a separate ZAAS at the root.
     */
    @Override
    protected String getInvalidateUrl(ServiceInstance instance) {
        return instance.getUri() + "/gateway/api/v1/auth/invalidate";
    }

}
