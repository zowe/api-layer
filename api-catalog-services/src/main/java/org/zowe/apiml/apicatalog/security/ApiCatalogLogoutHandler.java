/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;

@Slf4j
@RequiredArgsConstructor
public class ApiCatalogLogoutHandler implements LogoutHandler {

    private final CloseableHttpClient httpClient;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final String internalProtocol;
    private final String gatewayHostname;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String logoutUrl = authConfigurationProperties.getGatewayLogoutEndpoint();
        String gwHost = normalizeGatewayHost();

        HttpPost logoutRequest = new HttpPost(String.format("%s%s", gwHost, logoutUrl));
        setRequestHeaders(logoutRequest, request);
        try (CloseableHttpResponse logoutResponse = httpClient.execute(logoutRequest)) {
            if (logoutResponse.getStatusLine().getStatusCode() != SC_NO_CONTENT) {
                log.warn("Logout request to Gateway failed with status code {}: {}", logoutResponse.getStatusLine().getStatusCode(), logoutResponse.getEntity() != null ? EntityUtils.toString(logoutResponse.getEntity()) : "");
            }
        } catch (IOException e) {
            log.debug("I/O Exception in logout request: {}", e.getMessage(), e);
        }

    }

    private void setRequestHeaders(HttpPost logoutRequest, HttpServletRequest request) {
        String cookie = request.getHeader(HttpHeaders.COOKIE);
        if (StringUtils.isNotBlank(cookie)) {
            logoutRequest.addHeader(HttpHeaders.COOKIE, cookie);
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isNotBlank(authorization) && authorization.toLowerCase().startsWith("bearer ")) {
            logoutRequest.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    private String normalizeGatewayHost() {
        if (gatewayHostname.startsWith("https")) {
            return gatewayHostname.replaceFirst("https", internalProtocol);
        }
        if (gatewayHostname.startsWith("http")) {
            return gatewayHostname.replaceFirst("http", internalProtocol);
        }
        return String.format("%s://%s", internalProtocol, gatewayHostname);

    }

}
