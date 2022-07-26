/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.inmemory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.service.RejectStrategy;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.message.log.ApimlLogger;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RejectStrategyTest {
    private RejectStrategy underTest;

    @BeforeEach
    void setUp() {
        underTest = new RejectStrategy(ApimlLogger.empty());
    }

    @Test
    void evictThrowsException() {
        assertThrows(StorageException.class, () -> {
            underTest.evict("anyValue");
        });
    }
}
