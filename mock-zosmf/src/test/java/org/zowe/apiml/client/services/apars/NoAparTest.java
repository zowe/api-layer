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
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class NoAparTest {
    private NoApar underTest;

    @BeforeEach
    void setUp() {
        underTest = new NoApar();
    }

    @Test
    void givenParameters_RespondWithThird() {
        Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.OK));
        Optional<ResponseEntity<?>> actual = underTest.apply("first", "second", expected);

        assertThat(actual, is(expected));
    }
}
