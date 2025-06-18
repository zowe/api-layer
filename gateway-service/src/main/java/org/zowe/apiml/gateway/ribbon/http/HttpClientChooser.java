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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.zowe.apiml.gateway.security.service.schema.RoutingConstants;
import org.zowe.apiml.product.constants.CoreService;

import java.security.cert.X509Certificate;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;
import static org.zowe.apiml.gateway.security.service.schema.ByPassScheme.AUTHENTICATION_SCHEME_BY_PASS_KEY;

/**
 * Decides which HttpClient to use for HttpClientProxy method calls
 */
public class HttpClientChooser {

    private static final String CLIENT_CERT_HEADER = "Client-Cert";

    private final CloseableHttpClient clientWithoutCertificate;
    private final CloseableHttpClient clientWithCertificate;
    HttpClientChooser(CloseableHttpClient clientWithoutCertificate, CloseableHttpClient clientWithCertificate) {
        this.clientWithoutCertificate = clientWithoutCertificate;
        this.clientWithCertificate = clientWithCertificate;
    }

    private boolean isRequestToSign() {
        String serviceId = (String) RequestContext.getCurrentContext().get(SERVICE_ID_KEY);
        if (StringUtils.equalsAnyIgnoreCase(serviceId,
            CoreService.GATEWAY.getServiceId(),
            CoreService.CLOUD_GATEWAY.getServiceId()
        )) {
            /**
             * This is only a theoretical routing to another Gateway. It supports the trusted proxies and usage of
             * X-Forwarded-* headers. In theory just routing to the Cloud Gateway makes sense, even the aimed direction
             * is Cloud Gateway > Gateway > Service
             */
            if (RequestContext.getCurrentContext().getZuulRequestHeaders().get(CLIENT_CERT_HEADER) == null) {
                RequestContext.getCurrentContext().addZuulRequestHeader(CLIENT_CERT_HEADER, "");
            }

            return true;
        }

        if (!Boolean.TRUE.equals(RequestContext.getCurrentContext().get(AUTHENTICATION_SCHEME_BY_PASS_KEY))) {
            return false;
        }

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) return false;
        if (!(authentication.getCredentials() instanceof X509Certificate)) return false;

        return authentication.isAuthenticated();
    }

    public CloseableHttpClient chooseClient() {
        if (RequestContext.getCurrentContext().get(RoutingConstants.FORCE_CLIENT_WITH_APIML_CERT_KEY) != null) {
            return clientWithCertificate;
        }

        if (isRequestToSign()) {
            return clientWithCertificate;
        } else {
            return clientWithoutCertificate;
        }
    }
}
