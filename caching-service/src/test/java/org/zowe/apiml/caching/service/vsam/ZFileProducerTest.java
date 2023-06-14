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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.message.log.ApimlLogger;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ZFileProducerTest {
    private VsamConfig vsamConfiguration;
    private ZFileProducer underTest;

    @BeforeEach
    void setUp() {
        vsamConfiguration = DefaultVsamConfiguration.defaultConfiguration();
        underTest = new ZFileProducer(vsamConfiguration, VsamConfig.VsamOptions.WRITE, ApimlLogger.empty());
    }

    @Nested
    class WhenFileOpened {
        @Test
        void givenInvalidName_ExceptionIsThrown() {
            vsamConfiguration.setFileName("Invalid-file-name");
            assertThrows(IllegalStateException.class, () -> underTest.openZfile());
        }

        @Test
        void givenValidName_JzosNotFound() {
            // test code does not run with com.ibm.jzos available
            vsamConfiguration.setFileName("//'TEST'");
            assertThrows(JzosImplementationException.class, () -> underTest.openZfile());
        }
    }
}
