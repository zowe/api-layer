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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.login.x509.X509CommonNameUserMapper;
import org.zowe.apiml.security.common.token.QueryResponse;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.function.Supplier;

@Component
@Slf4j
public class X509Scheme implements AbstractAuthenticationScheme {

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.X509;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, Supplier<QueryResponse> token) {
        String[] headers = authentication.getHeaders().split(",");
        return new X509Command(headers);
    }

    public static class X509Command extends AuthenticationCommand {
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
            HttpServletRequest request = context.getRequest();
            X509Certificate[] certs = (X509Certificate[]) request.getAttribute("client.auth.X509Certificate");
            if (certs != null && certs.length > 0) {
                X509Certificate clientCert = certs[0];
                try {
                    setHeader(context, clientCert);
                } catch (CertificateEncodingException e) {
                    log.error("Exception parsing certificate", e);
                }
            }
        }

        private void setHeader(RequestContext context, X509Certificate clientCert) throws CertificateEncodingException {
            for (String header : headers) {
                switch (header.trim()) {
                    case PUBLIC_KEY:
                        String encodedCert = Base64.getEncoder().encodeToString(clientCert.getEncoded());
                        context.addZuulRequestHeader(PUBLIC_KEY, encodedCert);
                        continue;
                    case DISTINGUISHED_NAME:
                        String distinguishedName = clientCert.getSubjectDN().toString();
                        context.addZuulRequestHeader(DISTINGUISHED_NAME, distinguishedName);
                        continue;
                    case COMMON_NAME:
                        X509CommonNameUserMapper mapper = new X509CommonNameUserMapper();
                        String commonName = mapper.mapCertificateToMainframeUserId(clientCert);
                        context.addZuulRequestHeader(COMMON_NAME, commonName);
                }
            }
        }

        @Override
        public boolean isRequiredValidJwt() {
            return false;
        }

        @Override
        public boolean isExpired() {
            return false;
        }
    }
}
