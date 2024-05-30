/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.zowe.apiml.zaas.security.login.Providers;
import org.zowe.apiml.zaas.security.service.TokenCreationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthConfigValidationControllerTest {

    @InjectMocks
    private AuthConfigValidationController authConfigValidationController;

    @Mock
    private TokenCreationService tokenCreationService;

    @Mock
    private Providers providers;

    @Mock
    private Authentication authentication;

    @Nested
    class GivenRequestToValidateAuthConfig {

        @Nested
        class GivenUnauthenticated {

            @BeforeEach
            void setUp() {
                when(authentication.getPrincipal()).thenReturn(null);
            }

            @Test
            void whenZosmf_thenConflict() {
                when(providers.isZosfmUsed()).thenReturn(Boolean.TRUE);
                ResponseEntity<String> entity = authConfigValidationController.validateAuth(authentication);
                assertEquals(HttpStatusCode.valueOf(409), entity.getStatusCode());
                assertEquals("apimlAuthenticationToken cookie was not provided and a PassTicket cannot be generated with the z/OSMF provider", entity.getBody());
            }

            @Test
            void whenNotZosmf_thenTokenOk() {
                when(providers.isZosfmUsed()).thenReturn(Boolean.FALSE);
                when(tokenCreationService.createJwtTokenWithoutCredentials("validate")).thenReturn("token");
                ResponseEntity<String> entity = authConfigValidationController.validateAuth(authentication);
                assertEquals(HttpStatusCode.valueOf(200), entity.getStatusCode());
            }

            @Test
            void whenNotZosmf_thenTokenFail() {
                when(providers.isZosfmUsed()).thenReturn(Boolean.FALSE);
                when(tokenCreationService.createJwtTokenWithoutCredentials("validate")).thenThrow(new RuntimeException());
                ResponseEntity<String> entity = authConfigValidationController.validateAuth(authentication);
                assertEquals(HttpStatusCode.valueOf(409), entity.getStatusCode());
            }
        }

        @Nested
        class GivenAuthenticated {

            @BeforeEach
            void setUp() {
                when(authentication.getPrincipal()).thenReturn(new Object());
            }

            @Test
            void whenAuthConfigValidate_thenOk() {
                ResponseEntity<String> entity = authConfigValidationController.validateAuth(authentication);
                assertEquals(HttpStatusCode.valueOf(200), entity.getStatusCode());
            }

        }

    }

}
