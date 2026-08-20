/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth.saf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SafAuthorizationManagerTest {

    private static final String RESOURCE_CLASS = "ZOWE";
    private static final String RESOURCE_NAME = "APIML.SERVICES";
    private static final String RESOURCE_ACCESS = "READ";

    @Mock
    private SafResourceAccessVerifying safResourceAccessVerifying;

    @Mock
    private Authentication authentication;

    private SafAuthorizationManager<Object> safAuthorizationManager;

    @BeforeEach
    void setUp() {
        safAuthorizationManager = new SafAuthorizationManager<>(safResourceAccessVerifying, RESOURCE_CLASS, RESOURCE_NAME, RESOURCE_ACCESS);
    }

    @Nested
    class GivenSafAuthorization {

        @Test
        void whenAuthenticatedAndHasAccess_thenGrantWithAuthority() {
            when(authentication.isAuthenticated()).thenReturn(true);
            when(safResourceAccessVerifying.hasSafResourceAccess(authentication, RESOURCE_CLASS, RESOURCE_NAME, RESOURCE_ACCESS)).thenReturn(true);

            StepVerifier.create(safAuthorizationManager.check(Mono.just(authentication), new Object()))
                .assertNext(decision -> {
                    assertTrue(decision.isGranted());
                    var authorities = ((AuthorityAuthorizationDecision) decision).getAuthorities();
                    assertEquals(1, authorities.size());
                    assertEquals(String.format("%s.%s:%s", RESOURCE_CLASS, RESOURCE_NAME, RESOURCE_ACCESS), authorities.iterator().next().getAuthority());
                })
                .verifyComplete();
        }

        @Test
        void whenAuthenticatedButNoAccess_thenDenyWithoutAuthorities() {
            when(authentication.isAuthenticated()).thenReturn(true);
            when(safResourceAccessVerifying.hasSafResourceAccess(authentication, RESOURCE_CLASS, RESOURCE_NAME, RESOURCE_ACCESS)).thenReturn(false);

            StepVerifier.create(safAuthorizationManager.check(Mono.just(authentication), new Object()))
                .assertNext(decision -> {
                    assertFalse(decision.isGranted());
                    assertTrue(((AuthorityAuthorizationDecision) decision).getAuthorities().isEmpty());
                })
                .verifyComplete();
        }

        @Test
        void whenNotAuthenticated_thenDenyWithoutCallingVerifier() {
            when(authentication.isAuthenticated()).thenReturn(false);

            StepVerifier.create(safAuthorizationManager.check(Mono.just(authentication), new Object()))
                .assertNext(decision -> assertFalse(decision.isGranted()))
                .verifyComplete();

            verify(safResourceAccessVerifying, never()).hasSafResourceAccess(any(), any(), any(), any());
        }

    }

}
