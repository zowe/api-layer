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
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ZFileProducerTest {
    private ZFileProducer underTest;

    @BeforeEach
    void setUp() {
        VsamConfig vsamConfiguration = DefaultVsamConfiguration.defaultConfiguration();
        vsamConfiguration.setFileName("Invalid-file-name");
        underTest = new ZFileProducer(vsamConfiguration, VsamConfig.VsamOptions.WRITE);
    }

    @Test
    void givenInvaliName_whenFileOpened_ExceptionIsThrown() {
        assertThrows(IllegalStateException.class, () -> underTest.openZfile());
    }
}
