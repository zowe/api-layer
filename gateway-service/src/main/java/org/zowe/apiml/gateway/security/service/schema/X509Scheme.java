/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.schema;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;

import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.error.InvalidCertificateException;

/**
 * This schema adds requested information about client certificate. This information is added
 * to HTTP header. If no header is specified in Authentication object, empty command is returned.
 */
@Component
@Slf4j
public class X509Scheme implements IAuthenticationScheme {
    private final AuthSourceService authSourceService;
    private final MessageService messageService;

    public static final String AUTH_FAIL_HEADER = "X-Zowe-Auth-Failure";
    public static final String ALL_HEADERS = "X-Certificate-Public,X-Certificate-DistinguishedName,X-Certificate-CommonName";

    public X509Scheme(@Autowired @Qualifier("x509CNAuthSourceService") AuthSourceService authSourceService, MessageService messageService) {
        this.authSourceService = authSourceService;
        this.messageService = messageService;
    }

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.X509;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        String error;
        if (authSource == null || authSource.getRawSource() == null) {
            error = this.messageService.createMessage("org.zowe.apiml.gateway.security.schema.missingAuthentication").mapToLogMessage();
            return new X509Command(error);
        }

        X509AuthSource.Parsed parsedAuthSource;
        try {
            parsedAuthSource = (X509AuthSource.Parsed) authSourceService.parse(authSource);
        } catch (InvalidCertificateException e) {
            error = this.messageService.createMessage("org.zowe.apiml.gateway.security.scheme.x509ParsingError", e.getLocalizedMessage()).mapToLogMessage();
            return new X509Command(error);
        }

        if (parsedAuthSource == null) {
            error = this.messageService.createMessage("org.zowe.apiml.gateway.security.scheme.x509ParsingError", "Cannot parse provided authentication source").mapToLogMessage();
            return new X509Command(error);
        }

        String[] headers;
        if (StringUtils.isEmpty(authentication.getHeaders())) {
            headers = ALL_HEADERS.split(",");
        } else {
            headers = authentication.getHeaders().split(",");
        }

        Long expireAt = parsedAuthSource.getExpiration() != null ? parsedAuthSource.getExpiration().getTime() : null;
        return new X509Command(expireAt, headers, parsedAuthSource, null);
    }

    @Override
    public Optional<AuthSource> getAuthSource() {
        return authSourceService.getAuthSourceFromRequest();
    }

    public static class X509Command extends AuthenticationCommand {
        private final Long expireAt;
        private final String[] headers;
        private final X509AuthSource.Parsed parsedAuthSource;
        @Getter
        private final String errorHeader;

        public static final String PUBLIC_KEY = "X-Certificate-Public";
        public static final String DISTINGUISHED_NAME = "X-Certificate-DistinguishedName";
        public static final String COMMON_NAME = "X-Certificate-CommonName";

        public X509Command(Long expireAt, String[] headers, X509AuthSource.Parsed parsedAuthSource, String errorHeader) {
            this.expireAt = expireAt;
            this.headers = headers;
            this.parsedAuthSource = parsedAuthSource;
            this.errorHeader = errorHeader;
        }

        public X509Command(String errorHeader) {
            this.expireAt = null;
            this.headers = new String[0];
            this.parsedAuthSource = null;
            this.errorHeader = errorHeader;
        }

        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();
            if (parsedAuthSource != null) {
                setHeader(context, parsedAuthSource);
                context.set(RoutingConstants.FORCE_CLIENT_WITH_APIML_CERT_KEY);
            } else {
                setErrorHeader(context, errorHeader);
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
                        log.warn("Unsupported header specified in service metadata, " +
                            "please review apiml.service.authentication.headers, possible values are: " + PUBLIC_KEY +
                            ", " + DISTINGUISHED_NAME + ", " + COMMON_NAME + "\nprovided value: " + header);

                }
            }
        }

        private void setErrorHeader(RequestContext context, String value) {
            context.addZuulRequestHeader(AUTH_FAIL_HEADER, value);
            context.addZuulResponseHeader(AUTH_FAIL_HEADER, value);
        }
    }
}
