/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.schema.source;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.AuthSourceType;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Parsed;

import javax.servlet.http.HttpServletRequest;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Main implementation of AuthSourceService, supports four types of authentication source - JWT token, client certificate, personal access token and OIDC token.
 * <p>
 * Service keeps a map of the specific implementations of {@link AuthSourceService} which are responsible to perform operations defined by an interface
 * for a particular authentication source. {@link JwtAuthSourceService} is responsible for processing of the authentication source based on JWT token;
 *
 * @Qualifier("x509MFAuthSourceService") {@link X509AuthSourceService} is responsible for processing of the authentication source based on client certificate.
 * The key for the map is {@link AuthSourceType}.
 */
@Service
@Primary
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Slf4j
public class DefaultAuthSourceService implements AuthSourceService {
    private final Map<AuthSourceType, AuthSourceService> map = new EnumMap<>(AuthSourceType.class);

    private final boolean isX509Enabled;
    private final boolean isPATEnabled;
    private final boolean isOIDCEnabled;

    /**
     * Build the map of the specific implementations of {@link AuthSourceService} for processing of different type of authentications
     *
     * @param jwtAuthSourceService  {@link JwtAuthSourceService} service which process authentication source of type JWT
     * @param x509AuthSourceService {@link X509AuthSourceService} service which process authentication source of type client certificate
     * @param patAuthSourceService {@link PATAuthSourceService} service which process authentication source of type personal access token
     * @param isPATEnabled true if PAT is enabled as auth source
     * @param oidcAuthSourceService {@link OIDCAuthSourceService} service which process authentication source of type OIDC access token
     * @param isOIDCEnabled true if OIDC is enabled as auth source
     */
    public DefaultAuthSourceService(@Autowired JwtAuthSourceService jwtAuthSourceService,
                                    @Autowired @Qualifier("x509MFAuthSourceService") X509AuthSourceService x509AuthSourceService,
                                    @Value("${apiml.security.x509.enabled:false}") boolean isX509Enabled,
                                    PATAuthSourceService patAuthSourceService,
                                    @Value("${apiml.security.personalAccessToken.enabled:false}") boolean isPATEnabled,
                                    @Nullable OIDCAuthSourceService oidcAuthSourceService,
                                    @Value("${apiml.security.oidc.enabled:false}") boolean isOIDCEnabled) {
        this.isX509Enabled = isX509Enabled;
        this.isPATEnabled = isPATEnabled;
        this.isOIDCEnabled = isOIDCEnabled;
        map.put(AuthSourceType.JWT, jwtAuthSourceService);
        if (isX509Enabled) {
            map.put(AuthSourceType.CLIENT_CERT, x509AuthSourceService);
        }
        if (isPATEnabled) {
            map.put(AuthSourceType.PAT, patAuthSourceService);
        }
        if (isOIDCEnabled) {
            map.put(AuthSourceType.OIDC, oidcAuthSourceService);
        }
    }

    /**
     * Core method of the interface. Gets source of authentication from request.
     * <p>
     * In case if more than one source is present in request the precedence is the following:
     * 1) JWT token
     * 2) Client certificate
     * <p>
     *
     * @return Optional<AuthSource> which hold original source of authentication (JWT token, client certificate etc.)
     * or Optional.empty() when no authentication source found.
     */
    @Override
    public Optional<AuthSource> getAuthSourceFromRequest(HttpServletRequest request) {
        AuthSourceService service = getService(AuthSourceType.JWT);
        Optional<AuthSource> authSource = service.getAuthSourceFromRequest(request);
        String logMessage = "Authentication request towards the southbound service {} using the auth source {}";
        if (!authSource.isPresent() && isPATEnabled) {
            service = getService(AuthSourceType.PAT);
            authSource = service.getAuthSourceFromRequest(request);
        }
        if (!authSource.isPresent() && isOIDCEnabled) {
            service = getService(AuthSourceType.OIDC);
            authSource = service.getAuthSourceFromRequest(request);
        }
        if (!authSource.isPresent() && isX509Enabled) {
            service = getService(AuthSourceType.CLIENT_CERT);
            authSource = service.getAuthSourceFromRequest(request);
        }
        authSource.ifPresent(source -> log.debug(logMessage, request.getRequestURI(), source.getType().toString()));
        return authSource;
    }

    /**
     * Delegates the validation of the authentication source to a corresponding service.
     *
     * @param authSource {@link AuthSource} object which hold original source of authentication (JWT token, client certificate etc.)
     * @return true is authentication source is valid, false otherwise
     */
    @Override
    public boolean isValid(AuthSource authSource) {
        AuthSourceService service = getService(authSource);
        return service != null && service.isValid(authSource);
    }

    /**
     * Delegates the parsing of the authentication source to a corresponding service.
     *
     * @param authSource {@link AuthSource} object which hold original source of authentication (JWT token, client certificate etc.)
     * @return authentication source in parsed form
     */
    @Override
    public Parsed parse(AuthSource authSource) {
        AuthSourceService service = getService(authSource);
        return service != null ? service.parse(authSource) : null;
    }

    /**
     * Delegates the generation of the LTPA token based on the authentication source to a corresponding service.
     *
     * @param authSource {@link AuthSource} object which hold original source of authentication (JWT token, client certificate etc.)
     * @return LPTA token
     */
    @Override
    public String getLtpaToken(AuthSource authSource) {
        AuthSourceService service = getService(authSource);
        return service != null ? service.getLtpaToken(authSource) : null;
    }

    /**
     * Choose a service to process authentication source from the map of available services.
     *
     * @param authSource {@link AuthSource} object which hold original source of authentication (JWT token, client certificate etc.)
     * @return implementation of {@link AuthSourceService} or null if service not found
     */
    private AuthSourceService getService(AuthSource authSource) {
        return authSource != null ? getService(authSource.getType()) : null;
    }

    /**
     * Choose a service to process authentication source of specific type from the map of available services.
     *
     * @param authSourceType {@link AuthSourceType} type of the authentication source
     * @return implementation of {@link AuthSourceService} or null if service not found
     */
    private AuthSourceService getService(AuthSourceType authSourceType) {
        final AuthSourceService service = map.get(authSourceType);
        if (service == null) {
            throw new IllegalArgumentException("Unknown authentication source");
        }
        return service;
    }

    @Override
    public String getJWT(AuthSource authSource) {
        AuthSourceService service = getService(authSource);
        return service != null ? service.getJWT(authSource) : null;
    }
}
