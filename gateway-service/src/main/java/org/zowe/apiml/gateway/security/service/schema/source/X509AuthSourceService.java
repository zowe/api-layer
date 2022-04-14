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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.gateway.security.login.x509.X509AbstractMapper;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource.Parsed;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.error.InvalidCertificateException;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Optional;

/**
 * Basic implementation of AuthSourceService interface which uses client certificate as an authentication source.
 * This implementation relies on concrete implementation of {@link X509AbstractMapper} for validation and parsing of
 * the client certificate.
 */
@Slf4j
@RequiredArgsConstructor
public class X509AuthSourceService implements AuthSourceService {
    public static final String AUTH_FAIL_HEADER = "X-Zowe-Auth-Failure";
    private final X509AbstractMapper mapper;
    private final TokenCreationService tokenService;
    private final AuthenticationService authenticationService;
    protected final MessageService messageService;

    /**
     * Gets client certificate from request.
     * <p>
     * In case of multiple certificates only the first one will be used.
     * <p>
     *
     * @return Optional<AuthSource> with client certificate of Optional.empty()
     */
    @Override
    public Optional<AuthSource> getAuthSourceFromRequest() {
        final RequestContext context = RequestContext.getCurrentContext();
        X509Certificate clientCert = getCertificateFromRequest(context.getRequest(), "client.auth.X509Certificate");
        clientCert = checkCertificate(clientCert);
        return clientCert == null ? Optional.empty() : Optional.of(new X509AuthSource(clientCert));
    }

    /**
     * Check that certificate from request is not null and valid; otherwise set error header and do not use certificate for authentication
     * @param clientCert {@link X509Certificate} X509 client certificate.
     * @return client certificate if it is valid, otherwise null
     */
    protected X509Certificate checkCertificate(X509Certificate clientCert) {
        if (clientCert == null) {
            String error = this.messageService.createMessage("org.zowe.apiml.gateway.security.schema.missingAuthentication").mapToLogMessage();
            storeErrorHeader(error);
        } else {
            // check that X509 certificate is valid client certificate (has correct extended key usage)
            // if certificate is not valid - don't use it as a source of authentication
            clientCert = isValid(clientCert) ? clientCert : null;
        }
        return clientCert;
    }

    /**
     * Validates authentication source, check authentication source type and whether client certificate from the
     * authentication source has the extended key usage set correctly.
     *
     * @param authSource {@link AuthSource} object which hold original source of authentication - client certificate.
     * @return true if client certificate is valid, false otherwise.
     */
    public boolean isValid(AuthSource authSource) {
        if (authSource instanceof X509AuthSource) {
            X509Certificate clientCert = (X509Certificate) authSource.getRawSource();
            return isValid(clientCert);
        }
        return false;
    }

    /**
     * Validates X509 certificate, checks that certificate is not null and has the extended key usage set correctly.
     *
     * @param clientCert {@link X509Certificate} X509 client certificate.
     * @return true if client certificate is valid, false otherwise.
     */
    protected boolean isValid(X509Certificate clientCert) {
        if (clientCert == null) {
            return false;
        }

        try {
            if (mapper.isClientAuthCertificate(clientCert)) {
                return true;
            } else {
                String error = this.messageService.createMessage("org.zowe.apiml.gateway.security.scheme.x509ValidationError", "X509 certificate is missing the client certificate extended usage definition").mapToLogMessage();
                storeErrorHeader(error);
                return false;
            }
        } catch (Exception e) {
            String error = this.messageService.createMessage("org.zowe.apiml.gateway.security.scheme.x509ValidationError", e.getLocalizedMessage()).mapToLogMessage();
            storeErrorHeader(error);
            return false;
        }
    }

    /**
     * Parse client certificate from authentication source.
     *
     * @param authSource {@link AuthSource} object which hold original source of authentication - client certificate.
     * @return parsed authentication source or null if error occurred during parsing.
     */
    public AuthSource.Parsed parse(AuthSource authSource) {
        if (authSource instanceof X509AuthSource) {
            X509Certificate clientCert = (X509Certificate) authSource.getRawSource();
            return clientCert == null ? null : parseClientCert(clientCert, mapper);
        }
        return null;
    }

    @Override
    public String getLtpaToken(AuthSource authSource) {
        String jwt = getJWT(authSource);
        return jwt != null ? authenticationService.getLtpaToken(jwt) : null;
    }

    // Gets client certificate from request
    protected X509Certificate getCertificateFromRequest(HttpServletRequest request, String attributeName) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(attributeName);
        return getOne(certs);
    }

    protected X509Certificate getOne(X509Certificate[] certs) {
        if (certs != null && certs.length > 0) {
            return certs[0];
        } else return null;
    }

    /**
     * Parse client certificate: get common name, distinguished name and encoded certificate value.
     *
     * @param clientCert {@link X509Certificate} client certificate to parse.
     * @param mapper     instance of {@link X509AbstractMapper} to use for parsing.
     * @return parsed authentication source or null in case of CertificateEncodingException.
     */
    private Parsed parseClientCert(X509Certificate clientCert, X509AbstractMapper mapper) {
        try {
            String commonName = mapper.mapCertificateToMainframeUserId(clientCert);
            String encodedCert = Base64.getEncoder().encodeToString(clientCert.getEncoded());
            String distinguishedName = clientCert.getSubjectDN().toString();
            return new Parsed(commonName, clientCert.getNotBefore(), clientCert.getNotAfter(),
                Origin.X509, encodedCert, distinguishedName);
        } catch (CertificateEncodingException e) {
            log.error("Exception parsing certificate", e);
            throw new InvalidCertificateException("Exception parsing certificate. " + e.getLocalizedMessage()); // is this not in ticket?
        }
    }

    @Override
    public String getJWT(AuthSource authSource) {
        if (authSource instanceof X509AuthSource) {
            String userId = mapper.mapCertificateToMainframeUserId((X509Certificate) authSource.getRawSource());
            if (userId == null) {
                throw new UserNotMappedException("org.zowe.apiml.gateway.security.schema.x509.mappingFailed");
            }
            try {
                return tokenService.createJwtTokenWithoutCredentials(userId);
            } catch (Exception e) {
                throw new AuthenticationTokenException("org.zowe.apiml.gateway.security.token.authenticationFailed");
            }
        }
        return null;
    }

    // Method stores information about error into context to use it in header "X-Zowe-Auth-Failure"
    protected void storeErrorHeader(String value) {
        final RequestContext context = RequestContext.getCurrentContext();
        context.put(AUTH_FAIL_HEADER, value);
    }
}
