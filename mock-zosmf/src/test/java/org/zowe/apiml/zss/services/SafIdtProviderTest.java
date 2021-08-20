/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zss.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.zss.model.Authentication;
import org.zowe.apiml.zss.model.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class SafIdtProviderTest {
    private SafIdtProvider underTest;
    private Map<String, String> tokens;

    @BeforeEach
    void setUp() {
        tokens = new HashMap<>();
        underTest = new SafIdtProvider(tokens);
    }

    @Nested
    class WhenAuthenticating {
        @Nested
        class GivenAuthenticationWithUsername {
            @Test
            void tokenIsReturned() {
                Authentication authentication = new Authentication();
                authentication.setUsername("validUsername");

                Optional<Token> token = underTest.authenticate(authentication);

                assertThat(token.isPresent(), is(true));
            }
        }
    }

    @Nested
    class WhenVerifying {
        @Nested
        class GivenExistingToken {
            @Test
            void tokenIsVerified() {
                Token token = new Token();
                token.setJwt("username;validJwt");

                tokens.put("username", token.getJwt());

                assertThat(underTest.verify(token), is(true));
            }
        }
    }
}
