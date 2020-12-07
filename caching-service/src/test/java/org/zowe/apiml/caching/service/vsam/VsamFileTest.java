/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.vsam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.config.VsamConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VsamFileTest {

    VsamConfig config;

    @BeforeEach
    void prepareConfig() {
        config = mock(VsamConfig.class);
    }

    @Test
    void hasValidFileName() {
        when(config.getFileName()).thenReturn("//'DATASET.NAME'");
        assertThrows(UnsupportedOperationException.class, () -> new VsamFile(config));
    }

    @Test
    void hasInvalidFileName() {
        when(config.getFileName()).thenReturn("wrong");
        Exception exception = assertThrows(IllegalStateException.class,
            () -> new VsamFile(config),
            "Expected exception is not IllegalStateException");
        assertEquals("VsamFile does not exist", exception.getMessage());
    }
}
