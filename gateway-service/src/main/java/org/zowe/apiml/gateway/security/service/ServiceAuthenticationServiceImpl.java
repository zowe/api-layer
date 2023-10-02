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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.gateway.cache.RetryIfExpired;
import org.zowe.apiml.gateway.config.CacheConfig;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationCommand;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationSchemeFactory;
import org.zowe.apiml.gateway.security.service.schema.IAuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.ServiceAuthenticationService;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.util.CacheUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

/**
 * This bean is responsible for "translating" security to specific service. It decorates request with security data for
 * specific service. Implementation of security updates are defined with beans extending
 * {@link IAuthenticationScheme}.
 * <p>
 * The main idea of this bean is to create command
 * {@link AuthenticationCommand}. Command is object which update the
 * request for specific scheme (defined by service). This bean is responsible for getting the right command. It finds
 * just first authentication definition (there is an assumption that all instances of one service have the same
 * authentication scheme).
 * <p>
 * All those operation are cached:
 * - serviceAuthenticationByAuthentication (by authentication scheme and the source)
 * - serviceAuthenticationByServiceId (by serviceId, authentication scheme and the source)
 */
@Service
@RequiredArgsConstructor
public class ServiceAuthenticationServiceImpl implements ServiceAuthenticationService {

    private static final String CACHE_BY_SERVICE_ID = "serviceAuthenticationByServiceId";
    static final String CACHE_BY_AUTHENTICATION = "serviceAuthenticationByAuthentication";

    private final ApplicationContext applicationContext;
    private final EurekaClient discoveryClient;
    private final EurekaMetadataParser eurekaMetadataParser;
    private final AuthenticationSchemeFactory authenticationSchemeFactory;
    private final CacheManager cacheManager;
    private final CacheUtils cacheUtils;

    // to force calling inside methods with aspects - ie. ehCache aspect
    private ServiceAuthenticationService meAsProxy;

    @PostConstruct
    public void afterPropertiesSet() {
        meAsProxy = applicationContext.getBean(ServiceAuthenticationService.class);
    }

    public Authentication getAuthentication(InstanceInfo instanceInfo) {
        return eurekaMetadataParser.parseAuthentication(instanceInfo.getMetadata());
    }

    @Override
    public Authentication getAuthentication(String serviceId) {
        final Application application = discoveryClient.getApplication(serviceId);
        if (application == null) return null;

        final List<InstanceInfo> instances = application.getInstances();

        // iterates over all instances to verify if they all have the same authentication scheme in registration metadata
        for (final InstanceInfo instance : instances) {
            final Authentication auth = getAuthentication(instance);

            if (auth != null) {
                // this is the first record
                return auth;
            }
        }
        return null;
    }

    @Override
    @RetryIfExpired
    @CacheEvict(value = CACHE_BY_AUTHENTICATION, condition = "#result != null && #result.isExpired()")
    @Cacheable(value = CACHE_BY_AUTHENTICATION, unless = "#result == null")
    public AuthenticationCommand getAuthenticationCommand(Authentication authentication, AuthSource authSource) {
        final IAuthenticationScheme scheme = authenticationSchemeFactory.getSchema(authentication.getScheme());
        return scheme.createCommand(authentication, authSource);
    }

    @Override
    @RetryIfExpired
    @CacheEvict(
        value = CACHE_BY_SERVICE_ID,
        condition = "#result != null && #result.isExpired()",
        keyGenerator = CacheConfig.COMPOSITE_KEY_GENERATOR
    )
    @Cacheable(value = CACHE_BY_SERVICE_ID, keyGenerator = CacheConfig.COMPOSITE_KEY_GENERATOR, unless = "#result == null")
    public AuthenticationCommand getAuthenticationCommand(String serviceId, Authentication found, AuthSource authSource) {
        // if no instance exist or no metadata found, do nothing
        if (found == null || found.isEmpty()) return AuthenticationCommand.EMPTY;

        return meAsProxy.getAuthenticationCommand(found, authSource);
    }

    public Optional<AuthSource> getAuthSourceByAuthentication(Authentication authentication) {
        if (authentication == null || authentication.isEmpty()) {
            return Optional.empty();
        }
        final IAuthenticationScheme scheme = authenticationSchemeFactory.getSchema(authentication.getScheme());
        return scheme.getAuthSource();
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
