/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.gateway.cache.RetryIfExpired;
import org.zowe.apiml.gateway.config.CacheConfig;
import org.zowe.apiml.gateway.security.service.schema.AbstractAuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationCommand;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationSchemeFactory;
import org.zowe.apiml.gateway.security.service.schema.ServiceAuthenticationService;
import org.zowe.apiml.util.CacheUtils;

import java.util.List;

/**
 * This bean is responsible for "translating" security to specific service. It decorate request with security data for
 * specific service. Implementation of security updates are defined with beans extending
 * {@link AbstractAuthenticationScheme}.
 * <p>
 * The main idea of this bean is to create command
 * {@link AuthenticationCommand}. Command is object which update the
 * request for specific scheme (defined by service). This bean is responsible for getting the right command. If it is
 * possible to decide now for just one scheme type, bean return this command to update request immediatelly. Otherwise
 * it returns {@link LoadBalancerAuthenticationCommand}. This command write in the ZUUL context
 * UniversalAuthenticationCommand, which is used in Ribbon loadbalancer. There is a listener which work with this value.
 * After load balancer will decide which instance will be used, universal command is called and update the request.
 * <p>
 * All those operation are cached:
 * - serviceAuthenticationByAuthentication
 * - it caches command which can be deside only by serviceId (in pre filter)
 * - serviceAuthenticationByAuthentication
 * - it caches commands by {@link Authentication}, it could be in pre filters and
 * also in loadbalancer
 */
@Service
@AllArgsConstructor
public class ServiceAuthenticationServiceImpl implements ServiceAuthenticationService {

    public static final String AUTHENTICATION_COMMAND_KEY = "zoweAuthenticationCommand";

    private static final String CACHE_BY_SERVICE_ID = "serviceAuthenticationByServiceId";
    private static final String CACHE_BY_AUTHENTICATION = "serviceAuthenticationByAuthentication";

    private final EurekaClient discoveryClient;
    private final EurekaMetadataParser eurekaMetadataParser;
    private final AuthenticationSchemeFactory authenticationSchemeFactory;
    private final AuthenticationService authenticationService;
    private final CacheManager cacheManager;
    private final CacheUtils cacheUtils;

    public Authentication getAuthentication(InstanceInfo instanceInfo) {
        return eurekaMetadataParser.parseAuthentication(instanceInfo.getMetadata());
    }

    /**
     * This method is only for testing purpose, to be set authenticationService in inner classes
     *
     * @return reference for AuthenticationService
     */
    protected AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    @Override
    @CacheEvict(value = CACHE_BY_AUTHENTICATION, condition = "#result != null && #result.isExpired()")
    @Cacheable(CACHE_BY_AUTHENTICATION)
    public AuthenticationCommand getAuthenticationCommand(Authentication authentication, String jwtToken) {
        final AbstractAuthenticationScheme scheme = authenticationSchemeFactory.getSchema(authentication.getScheme());
        if (jwtToken == null) return scheme.createCommand(authentication, () -> null);
        return scheme.createCommand(authentication, () -> authenticationService.parseJwtToken(jwtToken));
    }

    @Override
    @RetryIfExpired
    @CacheEvict(
        value = CACHE_BY_SERVICE_ID,
        condition = "#result != null && #result.isExpired()",
        keyGenerator = CacheConfig.COMPOSITE_KEY_GENERATOR
    )
    @Cacheable(value = CACHE_BY_SERVICE_ID, keyGenerator = CacheConfig.COMPOSITE_KEY_GENERATOR)
    public AuthenticationCommand getAuthenticationCommand(String serviceId, String jwtToken) {
        final Application application = discoveryClient.getApplication(serviceId);
        if (application == null) return AuthenticationCommand.EMPTY;

        final List<InstanceInfo> instances = application.getInstances();
        Authentication auth = null;
//        If any instance is specifying which authentication it wants, then use the first found.
        for (InstanceInfo instance : instances) {
            auth = getAuthentication(instance);
            if (auth != null && !auth.isEmpty()) {
                break;
            }
        }
        if (auth == null || auth.isEmpty()) return AuthenticationCommand.EMPTY;
        return getAuthenticationCommand(auth, jwtToken);

    }

    @Override
    @CacheEvict(value = CACHE_BY_SERVICE_ID, allEntries = true)
    public void evictCacheAllService() {
        // evict all cached data accessible by serviceId
    }

    /**
     * Method evicts all records in cache serviceAuthenticationByServiceId, where is as key serviceId or records where
     * is impossible to known, which service is belongs to.
     *
     * @param serviceId Id of service to evict
     */
    @Override
    public void evictCacheService(String serviceId) {
        cacheUtils.evictSubset(cacheManager, CACHE_BY_SERVICE_ID, x -> StringUtils.equalsIgnoreCase((String) x.get(0), serviceId));
    }


}
