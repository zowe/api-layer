/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SemanticVersionTest {

    private SemanticVersion semanticVersion;

    @Nested
    class GivenVersionToCompare {
        @Test
        void whenVersionIsLower_thenReturnMinusOne() {
            semanticVersion = new SemanticVersion("1.0.0");
            int result = semanticVersion.compareTo(new SemanticVersion("2.0.0"));
            assertEquals(-1, result);
        }

        @Test
        void whenVersionIsHigher_thenReturnOne() {
            semanticVersion = new SemanticVersion("3.0.0");
            int result = semanticVersion.compareTo(new SemanticVersion("2.0.0"));
            assertEquals(1, result);
        }
    }


}
