/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.services.apars;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class JwtKeysTest {
    private JwtKeys underTest;

    @BeforeEach
    void setUp() {
        List<String> usernames = Collections.singletonList("USER");
        List<String> passwords = Collections.singletonList("validPassword");

        underTest = new JwtKeys(usernames, passwords);
    }

    @Nested
    class whenRetrieveJwtKeys {
        @Test
        void thenOkIsReturned() {
            Optional<ResponseEntity<?>> result = underTest.apply("jwtKeys", "get", null, null, null);

            assertThat(result.isPresent(), is(true));
            assertThat(result.get().getStatusCode(), is(HttpStatus.OK));
        }
    }
}
