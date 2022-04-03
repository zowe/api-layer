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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.login.x509.X509CommonNameUserMapper;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;

/**
 * This schema adds requested information about client certificate. This information is added
 * to HTTP header. If no header is specified in Authentication object, empty command is returned.
 */
@Component
@Slf4j
public class X509Scheme implements AbstractAuthenticationScheme {
    private final AuthSourceService authSourceService;

    public static final String ALL_HEADERS = "X-Certificate-Public,X-Certificate-DistinguishedName,X-Certificate-CommonName";

    public X509Scheme(@Autowired @Qualifier("x509CNAuthSourceService") AuthSourceService authSourceService) {
        this.authSourceService = authSourceService;
    }

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.X509;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        String[] headers;
        if (StringUtils.isEmpty(authentication.getHeaders())) {
            headers = ALL_HEADERS.split(",");
        } else {
            headers = authentication.getHeaders().split(",");
        }
        return new X509Command(headers);

    }

    @Override
    public Optional<AuthSource> getAuthSource() {
        return authSourceService.getAuthSourceFromRequest();
    }

    public class X509Command extends AuthenticationCommand {
        private final String[] headers;

        public static final String PUBLIC_KEY = "X-Certificate-Public";
        public static final String DISTINGUISHED_NAME = "X-Certificate-DistinguishedName";
        public static final String COMMON_NAME = "X-Certificate-CommonName";

        public X509Command(String[] headers) {
            this.headers = headers;
        }

        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();
            final AuthSource authSource = authSourceService.getAuthSourceFromRequest().orElse(null);
            X509Certificate clientCertificate = authSource == null ? null : (X509Certificate) authSource.getRawSource();

            if (clientCertificate != null) {
                try {
                    setHeader(context, clientCertificate);
                    context.set(RoutingConstants.FORCE_CLIENT_WITH_APIML_CERT_KEY);
                } catch (CertificateEncodingException e) {
                    log.error("Exception parsing certificate", e);
                }
            }
        }

        private void setHeader(RequestContext context, X509Certificate clientCert) throws CertificateEncodingException {
            for (String header : headers) {
                switch (header.trim()) {
                    case COMMON_NAME:
                        X509CommonNameUserMapper mapper = new X509CommonNameUserMapper();
                        String commonName = mapper.mapCertificateToMainframeUserId(clientCert);
                        context.addZuulRequestHeader(COMMON_NAME, commonName);
                        break;
                    case PUBLIC_KEY:
                        String encodedCert = Base64.getEncoder().encodeToString(clientCert.getEncoded());
                        context.addZuulRequestHeader(PUBLIC_KEY, encodedCert);
                        break;
                    case DISTINGUISHED_NAME:
                        String distinguishedName = clientCert.getSubjectDN().toString();
                        context.addZuulRequestHeader(DISTINGUISHED_NAME, distinguishedName);
                        break;
                    default:
                        log.warn("Unsupported header specified in service metadata, " +
                            "please review apiml.service.authentication.headers, possible values are: " + PUBLIC_KEY +
                            ", " + DISTINGUISHED_NAME + ", " + COMMON_NAME + "\nprovided value: " + header);

                }
            }
        }
    }
}
