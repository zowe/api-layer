/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package io.apiml.security.service.login.service;

import io.apiml.security.gateway.login.GatewayLoginRequest;
import io.apiml.security.gateway.login.GatewayLoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class GatewayUserService implements UserService {
    private final String loginUrl;
    private final RestTemplate restTemplate;

    public GatewayUserService(String loginUrl) {
        this.loginUrl = loginUrl;
        this.restTemplate = new RestTemplate();
        this.restTemplate.setErrorHandler(new UserServiceResponseErrorHandler());
    }

    @Override
    public String login(String username, String password) {
        HttpEntity<GatewayLoginRequest> request = new HttpEntity<>(new GatewayLoginRequest(username, password));
        ResponseEntity<GatewayLoginResponse> response = restTemplate
            .exchange(this.loginUrl, HttpMethod.POST, request, GatewayLoginResponse.class);
        return response.getBody().getApimlAuthenticationToken();
    }
}
