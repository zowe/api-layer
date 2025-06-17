/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.handler;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.gateway.service.InstanceInfoService;
import org.zowe.apiml.gateway.service.TokenProvider;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import reactor.core.publisher.Mono;

@Component
public class LocalTokenProvider extends TokenProvider {
    private final AuthenticationService authenticationService;

    public LocalTokenProvider(WebClient webClient, InstanceInfoService instanceInfoService, AuthenticationService authenticationService) {
        super(webClient, instanceInfoService);
        this.authenticationService = authenticationService;
    }

    @Override
    public Mono<QueryResponse> validateToken(String token) {
        return Mono.fromCallable(() -> {
            authenticationService.validateJwtToken(token);
            return authenticationService.parseJwtToken(token);
        }).onErrorResume(e ->
            Mono.error(new AuthenticationCredentialsNotFoundException("Token validation failed", e))
        );

    }

}
