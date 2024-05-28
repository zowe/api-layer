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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSchemeException;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.zaas.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.util.Optional;

/**
 * This schema adds requested information about client certificate. This information is added
 * to HTTP header. If no header is specified in Authentication object, empty command is returned.
 */
@Component
public class X509Scheme implements IAuthenticationScheme {
    @InjectApimlLogger
    private final ApimlLogger logger = ApimlLogger.empty();

    private final AuthSourceService authSourceService;
    private final AuthConfigurationProperties authConfigurationProperties;

    public static final String ALL_HEADERS = "X-Certificate-Public,X-Certificate-DistinguishedName,X-Certificate-CommonName";

    public X509Scheme(@Autowired @Qualifier("x509CNAuthSourceService") AuthSourceService authSourceService, AuthConfigurationProperties authConfigurationProperties) {
        this.authSourceService = authSourceService;
        this.authConfigurationProperties = authConfigurationProperties;
    }

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.X509;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        if (authSource == null || authSource.getRawSource() == null) {
            throw new AuthSchemeException("org.zowe.apiml.zaas.security.schema.missingX509Authentication");
        }

        X509AuthSource.Parsed parsedAuthSource = (X509AuthSource.Parsed) authSourceService.parse(authSource);
        if (parsedAuthSource == null) {
            throw new IllegalStateException("Error occurred while parsing the source of authentication.");
        }

        String[] headers;
        if (StringUtils.isEmpty(authentication.getHeaders())) {
            headers = ALL_HEADERS.split(",");
        } else {
            headers = authentication.getHeaders().split(",");
        }

        final long defaultExpirationTime = System.currentTimeMillis() + authConfigurationProperties.getX509Cert().getTimeout() * 1000L;
        final long expirationTime = parsedAuthSource.getExpiration() != null ? parsedAuthSource.getExpiration().getTime() : defaultExpirationTime;
        final long expireAt = Math.min(defaultExpirationTime, expirationTime);

        return new X509Command(expireAt, headers, parsedAuthSource);
    }

    @Override
    public Optional<AuthSource> getAuthSource() {
        return authSourceService.getAuthSourceFromRequest(RequestContext.getCurrentContext().getRequest());
    }

    public class X509Command extends AuthenticationCommand {
        private final Long expireAt;
        private final String[] headers;
        private final X509AuthSource.Parsed parsedAuthSource;

        public static final String PUBLIC_KEY = "X-Certificate-Public";
        public static final String DISTINGUISHED_NAME = "X-Certificate-DistinguishedName";
        public static final String COMMON_NAME = "X-Certificate-CommonName";

        public X509Command(Long expireAt, String[] headers, X509AuthSource.Parsed parsedAuthSource) {
            this.expireAt = expireAt;
            this.headers = headers;
            this.parsedAuthSource = parsedAuthSource;
        }

        @Override
        public void apply(InstanceInfo instanceInfo) {
            if (parsedAuthSource != null) {
                final RequestContext context = RequestContext.getCurrentContext();
                setHeader(context, parsedAuthSource);
                context.set(RoutingConstants.FORCE_CLIENT_WITH_APIML_CERT_KEY);
            }
        }

        @Override
        public boolean isExpired() {
            if (expireAt == null) return false;

            return System.currentTimeMillis() > expireAt;
        }

        private void setHeader(RequestContext context, X509AuthSource.Parsed parsedAuthSource) {
            for (String header : headers) {
                switch (header.trim()) {
                    case COMMON_NAME:
                        context.addZuulRequestHeader(COMMON_NAME, parsedAuthSource.getCommonName());
                        break;
                    case PUBLIC_KEY:
                        context.addZuulRequestHeader(PUBLIC_KEY, parsedAuthSource.getPublicKey());
                        break;
                    case DISTINGUISHED_NAME:
                        context.addZuulRequestHeader(DISTINGUISHED_NAME, parsedAuthSource.getDistinguishedName());
                        break;
                    default:
                        logger.log(MessageType.WARNING, "Unsupported header specified in service metadata, " +
                            "please review apiml.service.authentication.headers, possible values are: " + PUBLIC_KEY +
                            ", " + DISTINGUISHED_NAME + ", " + COMMON_NAME + "\nprovided value: " + header);

                }
            }
        }
    }
}
