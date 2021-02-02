/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.services.versions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zowe.apiml.client.services.apars.PH12143;
import org.zowe.apiml.client.services.apars.PHBase;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VersionsTest {
    private static final List<String> USERNAMES = Collections.singletonList("USER");
    private static final List<String> PASSWORDS = Collections.singletonList("validPassword");
    private Versions underTest;

    @BeforeEach
    void setUp() {
        underTest = new Versions(USERNAMES, PASSWORDS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.3", "2.4"})
    void givenVersion_whenGetBaseline_thenReturnPhBase(String version) throws Exception {
        List<Apar> result = underTest.baselineForVersion(version);

        assertEquals(result.size(), 1);
        assertTrue(result.stream().anyMatch(a -> a instanceof PHBase));
    }

    @Test
    void givenBadVersion_whenGetBaseline_thenThrowException() {
        assertThrows(Exception.class, () -> underTest.baselineForVersion("bad"));
    }

    @Test
    void givenVersionAndAppliedApar_whenGetAppliedApars_thenReturnAllApars() throws Exception {
        List<Apar> result = underTest.fullSetOfApplied("2.3", Collections.singletonList("PH12143"));
        assertTrue(result.size() > 1);
        assertTrue(result.stream().anyMatch(a -> a instanceof PH12143));
    }
}
