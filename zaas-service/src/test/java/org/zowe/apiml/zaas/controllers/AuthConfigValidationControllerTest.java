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
import org.zowe.apiml.zaas.security.login.Providers;
import org.zowe.apiml.zaas.security.service.TokenCreationService;

@ExtendWith(MockitoExtension.class)
public class AuthConfigValidationControllerTest {

    @InjectMocks
    private AuthConfigValidationController authConfigValidationController;

    @Mock
    private TokenCreationService tokenCreationService;

    @Mock
    private Providers providers;

    @BeforeEach
    void setUp() {

    }

    @Nested
    class GivenRequestToValidateAuthConfig {

        @Nested
        class GivenUnauthenticated {

            @Test
            void whenZosmf_thenConflict() {

            }

        }

        @Nested
        class GivenAuthenticated {

            void whenNotZosmf_thenToken() {

            }

        }

    }

}
