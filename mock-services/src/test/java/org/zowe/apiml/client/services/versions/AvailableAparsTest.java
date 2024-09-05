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
import org.zowe.apiml.client.services.apars.Apar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class AvailableAparsTest {
    private AvailableApars underTest;

    @BeforeEach
    void setUp() {
        underTest = new AvailableApars(Collections.singletonList("USER"), Collections.singletonList("validPassword"), "keystore/localhost/localhost.keystore.p12", 60);
    }

    @Test
    void givenAparsList_whenGetApars_returnAparsMatchedInList() {
        List<String> knownApars = new ArrayList<>();
        knownApars.add("PH12143");
        knownApars.add("JwtKeys");
        knownApars.add("AuthenticateApar");

        List<String> aparsToSearchFor = new ArrayList<>(knownApars);
        aparsToSearchFor.add("bad apar");

        List<Apar> result = underTest.getApars(aparsToSearchFor);

        assertEquals(result.size(), knownApars.size());
    }
}
