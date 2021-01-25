/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@RestController
@AllArgsConstructor
@RequestMapping("/gateway")
public class OldLogoutPathController {
    private final AuthConfigurationProperties authConfigurationProperties;
    private final RestTemplate restTemplate;
    private final AuthenticationService authenticationService;

    @PostMapping("/auth/logout")
    public void redirectToNewLogoutPath(HttpServletRequest request) {
        // Controller to proxy /api/v1/gateway/auth/logout as /gateway/api/v1/auth/logout, which is configured with Spring Security as the logout URL
        String url = String.format("%s://%s:%s%s",
            request.getScheme(),
            request.getServerName(),
            request.getServerPort(),
            authConfigurationProperties.getGatewayLogoutEndpoint()
        );
        Optional<String> token = authenticationService.getJwtTokenFromRequest(request);
        String cookie = String.format("%s=%s", authConfigurationProperties.getCookieProperties().getCookieName(), token.get());
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        restTemplate.postForObject(url, new HttpEntity<>(headers), String.class);
    }
}
