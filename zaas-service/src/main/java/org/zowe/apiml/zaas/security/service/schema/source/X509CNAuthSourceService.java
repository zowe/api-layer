/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema.source;

import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.zaas.security.mapping.X509CommonNameUserMapper;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.message.core.MessageType;

import jakarta.servlet.http.HttpServletRequest;

import java.security.cert.X509Certificate;
import java.util.Optional;

import static org.zowe.apiml.security.common.filter.CategorizeCertsFilter.ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE;
import static org.zowe.apiml.security.common.filter.CategorizeCertsFilter.ATTR_NAME_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE;

/**
 * Custom implementation of AuthSourceService interface which uses client certificate as an authentication source.
 * This implementation uses instance of {@link X509CommonNameUserMapper} for validation and parsing of the client certificate.
 */
@Slf4j
public class X509CNAuthSourceService extends X509AuthSourceService {
    public X509CNAuthSourceService(X509CommonNameUserMapper mapper, TokenCreationService tokenService, AuthenticationService authenticationService) {
        super(mapper, tokenService, authenticationService);
    }

    /**
     * Gets client certificate from request.
     * <p>
     * First try to get certificate from custom attribute.
     * If certificate not found - try to get it from standard attribute.
     * In case of multiple certificates only the first one will be used.
     * <p>
     *
     * @return Optional<AuthSource> with client certificate of Optional.empty()
     */
    @Override
    public Optional<AuthSource> getAuthSourceFromRequest(HttpServletRequest request) {
        logger.log(MessageType.DEBUG, "Getting X509 client certificate from custom attribute '" + ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE + "'.");
        X509Certificate clientCert = super.getCertificateFromRequest(request, ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE);
        if (clientCert == null) {
            logger.log(MessageType.DEBUG, "Getting X509 client certificate from standard attribute '" + ATTR_NAME_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE + "'.");
            clientCert = super.getCertificateFromRequest(request, ATTR_NAME_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE);
        }
        clientCert = isValid(clientCert) ? clientCert : null;
        return clientCert == null ? Optional.empty() : Optional.of(new X509AuthSource(clientCert));
    }

}
