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

import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.util.HttpUtils;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SuccessRefreshHandler implements ServerAuthenticationSuccessHandler {

    private final AuthenticationService authenticationService;
    private final HttpUtils httpUtils;
    private final TokenCreationService tokenCreationService;
    private final PeerAwareInstanceRegistryImpl peerAwareInstanceRegistry;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        if (authentication instanceof TokenAuthentication tokenAuthentication) {
            var app = peerAwareInstanceRegistry.getApplications().getRegisteredApplications(CoreService.GATEWAY.getServiceId());
            authenticationService.invalidateJwtTokenGateway(tokenAuthentication.getCredentials(), true, app);
            String jwtToken = tokenCreationService.createJwtTokenWithoutCredentials(tokenAuthentication.getPrincipal());
            exchange.getResponse().addCookie(httpUtils.createResponseCookie(jwtToken));
            return Mono.empty();
        }
        return webFilterExchange.getChain().filter(exchange);
    }
}
