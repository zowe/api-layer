/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.service;

import com.ca.apiml.security.common.auth.Authentication;
import com.ca.apiml.security.common.auth.AuthenticationScheme;
import com.ca.apiml.security.common.token.QueryResponse;
import com.ca.mfaas.gateway.config.CacheConfig;
import com.ca.mfaas.gateway.security.service.schema.AbstractAuthenticationScheme;
import com.ca.mfaas.gateway.security.service.schema.AuthenticationCommand;
import com.ca.mfaas.gateway.security.service.schema.AuthenticationSchemeFactory;
import com.ca.mfaas.gateway.security.service.schema.ServiceAuthenticationService;
import com.ca.mfaas.util.CacheUtils;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.zuul.context.RequestContext;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static com.ca.mfaas.constants.EurekaMetadataDefinition.AUTHENTICATION_APPLID;
import static com.ca.mfaas.constants.EurekaMetadataDefinition.AUTHENTICATION_SCHEME;

/**
 * This bean is responsible for "translating" security to specific service. It decorate request with security data for
 * specific service. Implementation of security updates are defined with beans extending
 * {@link com.ca.mfaas.gateway.security.service.schema.AbstractAuthenticationScheme}.
 *
 * The main idea of this bean is to create command
 * {@link com.ca.mfaas.gateway.security.service.schema.AuthenticationCommand}. Command is object which update the
 * request for specific scheme (defined by service). This bean is responsible for getting the right command. If it is
 * possible to decide now for just one scheme type, bean return this command to update request immediatelly. Otherwise
 * it returns {@link LoadBalancerAuthenticationCommand}. This command write in the ZUUL context
 * UniversalAuthenticationCommand, which is used in Ribbon loadbalancer. There is a listener which work with this value.
 * After load balancer will decide which instance will be used, universal command is called and update the request.
 *
 * All those operation are cached:
 *  - serviceAuthenticationByAuthentication
 *    - it caches command which can be deside only by serviceId (in pre filter)
 *  - serviceAuthenticationByAuthentication
 *    - it caches commands by {@link com.ca.apiml.security.common.auth.Authentication}, it could be in pre filters and
 *      also in loadbalancer
 */
@Service
@AllArgsConstructor
public class ServiceAuthenticationServiceImpl implements ServiceAuthenticationService {

    public static final String AUTHENTICATION_COMMAND_KEY = "zoweAuthenticationCommand";

    private static final String CACHE_BY_SERVICE_ID = "serviceAuthenticationByServiceId";
    private static final String CACHE_BY_AUTHENTICATION = "serviceAuthenticationByAuthentication";

    private final LoadBalancerAuthenticationCommand loadBalancerCommand = new LoadBalancerAuthenticationCommand();

    private final EurekaClient discoveryClient;
    private final AuthenticationSchemeFactory authenticationSchemeFactory;
    private final AuthenticationService authenticationService;
    private final CacheManager cacheManager;

    protected Authentication getAuthentication(InstanceInfo instanceInfo) {
        final Map<String, String> metadata = instanceInfo.getMetadata();

        final Authentication out = new Authentication();
        out.setApplid(metadata.get(AUTHENTICATION_APPLID));
        out.setScheme(AuthenticationScheme.fromScheme(metadata.get(AUTHENTICATION_SCHEME)));
        return out;
    }

    /**
     * This method is only for testing purpose, to be set authenticationService in inner classes
     * @return reference for AuthenticationService
     */
    protected AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    @Override
    @CacheEvict(value = CACHE_BY_AUTHENTICATION, condition = "#result != null && #result.isExpired()")
    @Cacheable(CACHE_BY_AUTHENTICATION)
    public AuthenticationCommand getAuthenticationCommand(Authentication authentication, String jwtToken) throws AuthenticationException {
        final AbstractAuthenticationScheme scheme = authenticationSchemeFactory.getSchema(authentication.getScheme());
        final QueryResponse queryResponse = authenticationService.parseJwtToken(jwtToken);
        return scheme.createCommand(authentication, queryResponse);
    }

    @Override
    @CacheEvict(value = CACHE_BY_SERVICE_ID, condition = "#result != null && #result.isExpired()")
    @Cacheable(value = CACHE_BY_SERVICE_ID, keyGenerator = CacheConfig.COMPOSITE_KEY_GENERATOR)
    public AuthenticationCommand getAuthenticationCommand(String serviceId, String jwtToken) throws AuthenticationException {
        final Application application = discoveryClient.getApplication(serviceId);
        if (application == null) return AuthenticationCommand.EMPTY;

        final List<InstanceInfo> instances = application.getInstances();

        Authentication found = null;
        for (final InstanceInfo instance : instances) {
            final Authentication auth = getAuthentication(instance);

            if (found == null) {
                // this is the first record
                found = auth;
            } else if (!found.equals(auth)) {
                // if next record is different, authentication cannot be determined before load balancer
                return loadBalancerCommand;
            }
        }

        // if no instance exist or no metadata found, do nothing
        if (found == null || found.isEmpty()) return AuthenticationCommand.EMPTY;

        return getAuthenticationCommand(found, jwtToken);
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
        CacheUtils.evictSubset(cacheManager, CACHE_BY_SERVICE_ID, x -> StringUtils.equalsIgnoreCase((String) x.get(0), serviceId));
    }

    public class UniversalAuthenticationCommand extends AuthenticationCommand {

        private static final long serialVersionUID = -2980076158001292742L;

        protected UniversalAuthenticationCommand() {}

        @Override
        public void apply(InstanceInfo instanceInfo) throws AuthenticationException {
            if (instanceInfo == null) throw new NullPointerException("Argument instanceInfo is required");

            final Authentication auth = getAuthentication(instanceInfo);
            final HttpServletRequest request = RequestContext.getCurrentContext().getRequest();

            final String jwtToken = getAuthenticationService().getJwtTokenFromRequest(request).orElse(null);

            final AuthenticationCommand cmd = getAuthenticationCommand(auth, jwtToken);
            cmd.apply(null);
        }

        @Override
        public boolean isExpired() {
            return false;
        }
    }

    public class LoadBalancerAuthenticationCommand extends AuthenticationCommand {

        private static final long serialVersionUID = 3363375706967769113L;

        private final UniversalAuthenticationCommand universal = new UniversalAuthenticationCommand();

        protected LoadBalancerAuthenticationCommand() {}

        @Override
        public void apply(InstanceInfo instanceInfo) {
            RequestContext.getCurrentContext().put(AUTHENTICATION_COMMAND_KEY, universal);
        }

        @Override
        public boolean isExpired() {
            return false;
        }

    }

}
