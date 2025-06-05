/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.auth.saf.AccessLevel;
import org.zowe.apiml.security.common.auth.saf.SafResourceAccessVerifying;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/gateway/auth")
@RequiredArgsConstructor
@SuppressWarnings("squid:S1075") // CONTEXT_PATH doesn't need to be parametrized
@Slf4j
public class ReactiveSafResourceAccessController {

    public static final String CONTEXT_PATH = "/check";

    private final SafResourceAccessVerifying safResourceAccessVerifying;
    private final MessageService messageService;

    @PostMapping(path = CONTEXT_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> hasSafAccess(@RequestBody CheckRequestModel request) {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> Mono.justOrEmpty(ctx.getAuthentication()))
            .filter(auth -> auth.getPrincipal() != null)
            .flatMap(authentication -> {
                if (safResourceAccessVerifying.hasSafResourceAccess(
                    authentication,
                    request.getResourceClass(),
                    request.getResourceName(),
                    request.getAccessLevel().name()
                )) {
                    return Mono.just(ResponseEntity.noContent().build());
                } else {
                    log.debug("Access denied for user: {}", authentication.getPrincipal());
                    return Mono.just(
                        new ResponseEntity<>(
                            messageService.createMessage(
                                "org.zowe.apiml.security.unauthorized",
                                authentication.getPrincipal().toString()
                            ).mapToApiMessage(),
                            HttpStatus.UNAUTHORIZED
                        )
                    );
                }
            })
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatusCode.valueOf(401)).build()));
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    static class CheckRequestModel {
        private String resourceClass;
        private String resourceName;
        private AccessLevel accessLevel;
    }
}
