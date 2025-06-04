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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.zowe.apiml.message.api.ApiMessage;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.auth.saf.AccessLevel;
import org.zowe.apiml.security.common.auth.saf.SafResourceAccessVerifying;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveSafResourceAccessControllerTest {

    @Mock
    private SafResourceAccessVerifying safResourceAccessVerifying;
    @Mock
    private MessageService messageService;
    @InjectMocks
    private ReactiveSafResourceAccessController controller;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private final ReactiveSafResourceAccessController.CheckRequestModel validRequest =
        new ReactiveSafResourceAccessController.CheckRequestModel("ZOWE", "APIML.SERVICES", AccessLevel.READ);

    @Test
    void testHasAccess_shouldReturnNoContent() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("USER1");
        when(safResourceAccessVerifying.hasSafResourceAccess(any(), eq("ZOWE"), eq("APIML.SERVICES"), eq("READ")))
            .thenReturn(true);

        try (MockedStatic<ReactiveSecurityContextHolder> mocked = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
            mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

            Mono<ResponseEntity<?>> result = controller.hasSafAccess(validRequest);

            StepVerifier.create(result)
                .expectNextMatches(response -> response.getStatusCode().equals(HttpStatus.NO_CONTENT))
                .verifyComplete();
        }
    }

    @Test
    void testNoAccess_shouldReturnUnauthorized() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("USER2");
        when(safResourceAccessVerifying.hasSafResourceAccess(any(), eq("ZOWE"), eq("APIML.SERVICES"), eq("READ")))
            .thenReturn(false);

        Message mockMessage = mock(Message.class);
        ApiMessage apiMessage = new ApiMessage("org.zowe.apiml.security.unauthorized", null, null, null, null, null);
        when(messageService.createMessage("org.zowe.apiml.security.unauthorized", "USER2")).thenReturn(mockMessage);
        when(mockMessage.mapToApiMessage()).thenReturn(apiMessage);

        try (MockedStatic<ReactiveSecurityContextHolder> mocked = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
            mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

            Mono<ResponseEntity<?>> result = controller.hasSafAccess(validRequest);

            StepVerifier.create(result)
                .expectNextMatches(response ->
                    response.getStatusCode().equals(HttpStatus.UNAUTHORIZED) &&
                        response.getBody().equals(apiMessage)
                )
                .verifyComplete();
        }
    }

    @Test
    void testNoAuthContext_shouldReturnUnauthorized() {
        try (MockedStatic<ReactiveSecurityContextHolder> mocked = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
            mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.empty());

            Mono<ResponseEntity<?>> result = controller.hasSafAccess(validRequest);

            StepVerifier.create(result)
                .expectNextMatches(response -> response.getStatusCode().equals(HttpStatus.UNAUTHORIZED))
                .verifyComplete();
        }
    }

    @Test
    void testNullAuthentication_shouldReturnUnauthorized() {
        when(securityContext.getAuthentication()).thenReturn(null);

        try (MockedStatic<ReactiveSecurityContextHolder> mocked = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
            mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

            Mono<ResponseEntity<?>> result = controller.hasSafAccess(validRequest);

            StepVerifier.create(result)
                .expectNextMatches(response -> response.getStatusCode().equals(HttpStatus.UNAUTHORIZED))
                .verifyComplete();
        }
    }

    @Test
    void testNullPrincipal_shouldReturnUnauthorized() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(null);

        try (MockedStatic<ReactiveSecurityContextHolder> mocked = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
            mocked.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));

            Mono<ResponseEntity<?>> result = controller.hasSafAccess(validRequest);

            StepVerifier.create(result)
                .expectNextMatches(response -> response.getStatusCode().equals(HttpStatus.UNAUTHORIZED))
                .verifyComplete();
        }
    }
}
