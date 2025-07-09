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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.util.CacheUtils;
import org.zowe.apiml.util.EurekaUtils;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;

@Slf4j
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ConditionalOnBean(name = "modulithConfig")
public class ModulithAuthenticationService extends AuthenticationService {

    public ModulithAuthenticationService(ApplicationContext applicationContext,
            AuthConfigurationProperties authConfigurationProperties, JwtSecurity jwtSecurityInitializer,
            ZosmfService zosmfService, EurekaClient eurekaClient, RestTemplate restTemplate, CacheManager cacheManager,
            CacheUtils cacheUtils) {
        super(applicationContext, authConfigurationProperties, jwtSecurityInitializer, zosmfService, eurekaClient, restTemplate,
                cacheManager, cacheUtils);
    }

    @Override
    protected String getInvalidateUrl(InstanceInfo instanceInfo, String jwtToken) {
        return EurekaUtils.getUrl(instanceInfo) + "/gateway/api/v1/auth/invalidate/" + jwtToken;
    }

}
