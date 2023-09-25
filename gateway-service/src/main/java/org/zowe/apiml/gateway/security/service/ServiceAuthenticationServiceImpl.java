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
import com.netflix.loadbalancer.reactive.ExecutionListener;
import com.netflix.zuul.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.gateway.cache.RetryIfExpired;
import org.zowe.apiml.gateway.config.CacheConfig;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationCommand;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationSchemeFactory;
import org.zowe.apiml.gateway.security.service.schema.IAuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.ServiceAuthenticationService;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSchemeException;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.util.CacheUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

/**
 * This bean is responsible for "translating" security to specific service. It decorate request with security data for
 * specific service. Implementation of security updates are defined with beans extending
 * {@link IAuthenticationScheme}.
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
@RequiredArgsConstructor
public class ServiceAuthenticationServiceImpl implements ServiceAuthenticationService {

    public static final String AUTHENTICATION_COMMAND_KEY = "zoweAuthenticationCommand";

    private static final String CACHE_BY_SERVICE_ID = "serviceAuthenticationByServiceId";
    static final String CACHE_BY_AUTHENTICATION = "serviceAuthenticationByAuthentication";

    private final LoadBalancerAuthenticationCommand loadBalancerCommand = new LoadBalancerAuthenticationCommand();
    private final LoadBalancerAuthentication loadBalancerAuthentication = new LoadBalancerAuthentication();

    private final ApplicationContext applicationContext;
    private final EurekaClient discoveryClient;
    private final EurekaMetadataParser eurekaMetadataParser;
    private final AuthenticationSchemeFactory authenticationSchemeFactory;
    private final AuthSourceService authSourceService;
    private final CacheManager cacheManager;
    private final CacheUtils cacheUtils;

    // to force calling inside methods with aspects - ie. ehCache aspect
    private ServiceAuthenticationService meAsProxy;

    @PostConstruct
    public void afterPropertiesSet() {
        meAsProxy = applicationContext.getBean(ServiceAuthenticationService.class);
    }


    /**
     * Marker type of Authentication, the sole purpose of it is to highlight the fact that
     * authentication cannot be determined before load balancer
     */
    static class LoadBalancerAuthentication extends Authentication {}

    public Authentication getAuthentication(InstanceInfo instanceInfo) {
        return eurekaMetadataParser.parseAuthentication(instanceInfo.getMetadata());
    }

    @Override
    public Authentication getAuthentication(String serviceId) {
        final Application application = discoveryClient.getApplication(serviceId);
        if (application == null) return null;

        final List<InstanceInfo> instances = application.getInstances();

        Authentication found = null;
        // iterates over all instances to verify if they all have the same authentication scheme in registration metadata
        for (final InstanceInfo instance : instances) {
            final Authentication auth = getAuthentication(instance);

            if (found == null) {
                // this is the first record
                found = auth;
            } else if (!found.equals(auth)) {
                // if next record is different, authentication cannot be determined before load balancer and
                // will be selected in load balancer with applyToRequest method
                return loadBalancerAuthentication;
            }
        }
        return found;
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
        // Authentication cannot be determined before load balancer
        if (found instanceof LoadBalancerAuthentication) return loadBalancerCommand;
        // if no instance exist or no metadata found, do nothing
        if (found == null || found.isEmpty()) return AuthenticationCommand.EMPTY;

        return meAsProxy.getAuthenticationCommand(found, authSource);
    }

    public Optional<AuthSource> getAuthSourceByAuthentication(Authentication authentication) {
        if (authentication == null || authentication.isEmpty() || authentication instanceof LoadBalancerAuthentication) {
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

    public class UniversalAuthenticationCommand extends AuthenticationCommand {

        private static final long serialVersionUID = -2980076158001292742L;

        private static final String INVALID_JWT_MESSAGE = "Invalid JWT token";

        protected UniversalAuthenticationCommand() {
        }

        @Override
        public void apply(InstanceInfo instanceInfo) {
            if (instanceInfo == null) throw new NullPointerException("Argument instanceInfo is required");

            final Authentication auth = getAuthentication(instanceInfo);

            AuthenticationCommand cmd = null;

            boolean rejected = false;
            try {
                final Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest();
                cmd = meAsProxy.getAuthenticationCommand(auth, authSource.orElse(null));

                // if authentication schema required valid authentication source, check it
                if (cmd.isRequiredValidSource()) {
                    rejected = (!authSource.isPresent()) || !authSourceService.isValid(authSource.get());
                }

            } catch (AuthenticationException | AuthSchemeException ae) {
                rejected = true;
            }

            if (rejected) {
                throw new ExecutionListener.AbortExecutionException(INVALID_JWT_MESSAGE, new BadCredentialsException(INVALID_JWT_MESSAGE));
            }

            cmd.apply(null);
        }
    }

    public class LoadBalancerAuthenticationCommand extends AuthenticationCommand {

        private static final long serialVersionUID = 3363375706967769113L;

        private final UniversalAuthenticationCommand universal = new UniversalAuthenticationCommand();

        protected LoadBalancerAuthenticationCommand() {
        }

        @Override
        public void apply(InstanceInfo instanceInfo) {
            RequestContext.getCurrentContext().put(AUTHENTICATION_COMMAND_KEY, universal);
        }
    }

}
