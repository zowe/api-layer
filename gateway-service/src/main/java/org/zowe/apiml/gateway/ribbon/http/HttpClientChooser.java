/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.http;

import com.netflix.zuul.context.RequestContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.cert.X509Certificate;

import static org.zowe.apiml.gateway.security.service.schema.ByPassScheme.AUTHENTICATION_SCHEME_BY_PASS_KEY;

/**
 * Decides which HttpClient to use for HttpClientProxy method calls
 */
public class HttpClientChooser {

    private final CloseableHttpClient clientWithoutCertificate;
    private final CloseableHttpClient clientWithCertificate;
    HttpClientChooser(CloseableHttpClient clientWithoutCertificate, CloseableHttpClient clientWithCertificate) {
        this.clientWithoutCertificate = clientWithoutCertificate;
        this.clientWithCertificate = clientWithCertificate;
    }

    private boolean isRequestToSign() {
        if (!Boolean.TRUE.equals(RequestContext.getCurrentContext().get(AUTHENTICATION_SCHEME_BY_PASS_KEY))) {
            return false;
        }

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) return false;
        if (!(authentication.getCredentials() instanceof X509Certificate)) return false;

        return authentication.isAuthenticated();
    }

    public CloseableHttpClient chooseClient() {
        if (isRequestToSign()) {
            return clientWithCertificate;
        } else {
            return clientWithoutCertificate;
        }
    }
}
