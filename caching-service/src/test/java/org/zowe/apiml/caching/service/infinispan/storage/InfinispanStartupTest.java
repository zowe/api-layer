/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.infinispan.storage;

import org.infinispan.manager.DefaultCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {"caching.storage.mode=infinispan",
    "jgroups.bind.port=7099", "jgroups.bind.address=localhost", "apiml.enabled=false"})
class InfinispanStartupTest {

    @Autowired
    DefaultCacheManager cacheManager;

    @Test
    void whenCacheIsRequested_thenReturnNotNull() {
        assertNotNull(cacheManager.getCache("zoweCache"));
    }
}
