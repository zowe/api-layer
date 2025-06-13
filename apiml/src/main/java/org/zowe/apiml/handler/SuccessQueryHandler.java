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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Service;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SuccessQueryHandler implements ServerAuthenticationSuccessHandler {

    private final ObjectMapper mapper;
    private final AuthenticationService authenticationService;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
        String token = tokenAuthentication.getCredentials();
        var bufferFactory = new DefaultDataBufferFactory();

        webFilterExchange.getExchange().getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        webFilterExchange.getExchange().getResponse().setStatusCode(HttpStatus.OK);
        DefaultDataBuffer buffer;
        try {
            buffer = bufferFactory.wrap(mapper.writeValueAsBytes(authenticationService.parseJwtToken(token)));
            return webFilterExchange.getExchange().getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }

    }
}
