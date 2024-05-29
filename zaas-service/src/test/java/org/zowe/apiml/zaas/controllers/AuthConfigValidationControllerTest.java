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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.zaas.security.login.Providers;
import org.zowe.apiml.zaas.security.service.TokenCreationService;

@ExtendWith(SpringExtension.class)
public class AuthConfigValidationControllerTest {

    @Mock
    private TokenCreationService tokenCreationService;

    @Mock
    private Providers providers;

    @BeforeEach
    void setUp() {

    }

}
