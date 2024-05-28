/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSchemeException;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.util.Optional;

@Component
public class ZoweJwtScheme implements IAuthenticationScheme {
    @InjectApimlLogger
    private final ApimlLogger logger = ApimlLogger.empty();

    private final AuthSourceService authSourceService;
    private final AuthConfigurationProperties configurationProperties;

    @Value("${apiml.security.auth.jwt.customAuthHeader:}")
    private String customHeader;

    @Autowired
    public ZoweJwtScheme(AuthSourceService authSourceService, AuthConfigurationProperties configurationProperties) {
        this.authSourceService = authSourceService;
        this.configurationProperties = configurationProperties;
    }

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.ZOWE_JWT;
    }

    @Override
    public Optional<AuthSource> getAuthSource() {
        return authSourceService.getAuthSourceFromRequest(RequestContext.getCurrentContext().getRequest());
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        if (authSource == null || authSource.getRawSource() == null) {
            throw new AuthSchemeException("org.zowe.apiml.zaas.security.schema.missingAuthentication");
        }

        String jwt;
        AuthSource.Parsed parsedAuthSource;
        try {
            parsedAuthSource = authSourceService.parse(authSource);
            if (parsedAuthSource == null) {
                throw new IllegalStateException("Error occurred while parsing authenticationSource");
            }
            jwt = authSourceService.getJWT(authSource);
        } catch (TokenNotValidException e) {
            logger.log(MessageType.DEBUG, e.getLocalizedMessage());
            throw new AuthSchemeException("org.zowe.apiml.zaas.security.invalidToken");
        } catch (TokenExpireException e) {
            logger.log(MessageType.DEBUG, e.getLocalizedMessage());
            throw new AuthSchemeException("org.zowe.apiml.zaas.security.expiredToken");
        }

        final long defaultExpirationTime = System.currentTimeMillis() + configurationProperties.getTokenProperties().getExpirationInSeconds() * 1000L;
        final long expirationTime = parsedAuthSource.getExpiration() != null ? parsedAuthSource.getExpiration().getTime() : defaultExpirationTime;
        final long expireAt = Math.min(defaultExpirationTime, expirationTime);

        return new ZoweJwtAuthCommand(expireAt, jwt);
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false)
    public class ZoweJwtAuthCommand extends JwtCommand {

        public static final long serialVersionUID = -885301934611866658L;
        Long expireAt;
        String jwt;

        @Override
        public void apply(InstanceInfo instanceInfo) {
            if (jwt != null) {
                final RequestContext context = RequestContext.getCurrentContext();
                JwtCommand.setCustomHeader(context, HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
                JwtCommand.setCookie(context, configurationProperties.getCookieProperties().getCookieName(), jwt);
                if (StringUtils.isNotEmpty(customHeader)) {
                    JwtCommand.setCustomHeader(context, customHeader, jwt);
                }
            }
        }

    }
}
