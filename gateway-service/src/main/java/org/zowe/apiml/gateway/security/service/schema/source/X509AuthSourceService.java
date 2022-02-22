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

import com.netflix.zuul.context.RequestContext;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.security.login.x509.X509AbstractMapper;
import org.zowe.apiml.gateway.security.login.x509.X509CommonNameUserMapper;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource.Parsed;

/**
 * Implementation of AuthSourceService interface which uses client certificate as an authentication source.
 * This implementation of service does not perform  mapping between common name from the client certificate and
 * the mainframe user and treats common name as user ID.
 */
@Slf4j
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class X509AuthSourceService implements AuthSourceService {
    private X509AbstractMapper mapper;

    public X509AuthSourceService() {
        mapper = new X509CommonNameUserMapper();
    }

    // Method for testing purpose only
    protected void setMapper(X509AbstractMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Returns client certificate from request or Optional.empty() if the certificate not found.
     * In case of multiple certificates only the first one will be used.
     * @return client certificate of Optional.empty()
     */
    @Override
    public Optional<AuthSource> getAuthSourceFromRequest() {
        final RequestContext context = RequestContext.getCurrentContext();

        X509Certificate clientCert = getCertificateFromRequest(context.getRequest());
        return clientCert == null ? Optional.empty() : Optional.of(new X509AuthSource(clientCert));
    }

    /**
     * Validates authentication source, check authentication source type and whether client certificate from the
     * authentication source has the extended key usage set correctly.
     * @param authSource AuthSource object which hold original source of authentication - client certificate.
     * @return true if client certificate has valid, false otherwise
     */
    @Override
    public boolean isValid(AuthSource authSource) {
        if (authSource instanceof X509AuthSource) {
            X509Certificate clientCert = (X509Certificate)authSource.getRawSource();
            return  clientCert != null && mapper.isClientAuthCertificate(clientCert);
        }
        return false;
    }

    /**
     * Return authentication source in parsed form if source is of correct type and client certificate from the
     * authentication source is not null.
     * @param authSource AuthSource object which hold original source of authentication (JWT token, client certificate etc.)
     * @return parsed authentication source or null.
     */
    @Override
    public AuthSource.Parsed parse(AuthSource authSource) {
        if (authSource instanceof X509AuthSource) {
            X509Certificate clientCert = (X509Certificate)authSource.getRawSource();
            return clientCert == null ? null : parseClientCert(clientCert);
        }
        return null;
    }

    @Override
    public String getLtpaToken(AuthSource authSource) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    private X509Certificate getCertificateFromRequest(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("client.auth.X509Certificate");
        return getOne(certs);
    }

    private X509Certificate getOne(X509Certificate[] certs) {
        if (certs != null && certs.length > 0) {
            return certs[0];
        } else return null;
    }

    private Parsed parseClientCert(X509Certificate clientCert) {
        try {
            String commonName = mapper.mapCertificateToMainframeUserId(clientCert);
            String encodedCert = Base64.getEncoder().encodeToString(clientCert.getEncoded());
            String distinguishedName = clientCert.getSubjectDN().toString();
            return new Parsed(commonName, clientCert.getNotBefore(), clientCert.getNotAfter(),
                null, encodedCert, distinguishedName);
        } catch (CertificateEncodingException e) {
            log.error("Exception parsing certificate", e);
        }
        return null;
    }
}
