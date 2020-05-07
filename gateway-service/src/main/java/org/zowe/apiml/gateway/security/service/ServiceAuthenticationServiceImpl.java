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
import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.config.CacheConfig;
import org.zowe.apiml.gateway.security.service.schema.AbstractAuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationCommand;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationSchemeFactory;
import org.zowe.apiml.gateway.security.service.schema.ServiceAuthenticationService;
import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.util.CacheUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_APPLID;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_SCHEME;

/**
 * This bean is responsible for "translating" security to specific service. It decorate request with security data for
 * specific service. Implementation of security updates are defined with beans extending
 * {@link AbstractAuthenticationScheme}.
 *
 * The main idea of this bean is to create command
 * {@link AuthenticationCommand}. Command is object which update the
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
 *    - it caches commands by {@link Authentication}, it could be in pre filters and
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

    public Authentication getAuthentication(InstanceInfo instanceInfo) {
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
    public AuthenticationCommand getAuthenticationCommand(Authentication authentication, String jwtToken) {
        final AbstractAuthenticationScheme scheme = authenticationSchemeFactory.getSchema(authentication.getScheme());
        if (jwtToken == null) return scheme.createCommand(authentication, () -> null);
        return scheme.createCommand(authentication, () -> authenticationService.parseJwtToken(jwtToken));
    }

    @Override
    @CacheEvict(value = CACHE_BY_SERVICE_ID, condition = "#result != null && #result.isExpired()")
    @Cacheable(value = CACHE_BY_SERVICE_ID, keyGenerator = CacheConfig.COMPOSITE_KEY_GENERATOR)
    public AuthenticationCommand getAuthenticationCommand(String serviceId, String jwtToken) {
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

        private static final String INVALID_JWT_MESSAGE = "Invalid JWT token";

        protected UniversalAuthenticationCommand() {}

        @Override
        public void apply(InstanceInfo instanceInfo) {
            if (instanceInfo == null) throw new NullPointerException("Argument instanceInfo is required");

            final Authentication auth = getAuthentication(instanceInfo);
            final RequestContext requestContext = RequestContext.getCurrentContext();
            final HttpServletRequest request = requestContext.getRequest();

            AuthenticationCommand cmd = null;

            boolean rejected = false;
            try {
                final String jwtToken = getAuthenticationService().getJwtTokenFromRequest(request).orElse(null);
                cmd = getAuthenticationCommand(auth, jwtToken);

                // if authentication schema required valid JWT, check it
                if (cmd.isRequiredValidJwt()) {
                    rejected = (jwtToken == null) || !authenticationService.validateJwtToken(jwtToken).isAuthenticated();
                }

            } catch (AuthenticationException ae) {
                rejected = true;
            }

            if (rejected) {
                throw new ExecutionListener.AbortExecutionException(INVALID_JWT_MESSAGE, new BadCredentialsException(INVALID_JWT_MESSAGE));
            }

            cmd.apply(null);
        }

        @Override
        public boolean isExpired() {
            return false;
        }

        @Override
        public boolean isRequiredValidJwt() {
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

        @Override
        public boolean isRequiredValidJwt() {
            return false;
        }

    }

}
