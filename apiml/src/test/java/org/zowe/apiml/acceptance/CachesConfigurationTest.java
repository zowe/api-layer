/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import org.infinispan.manager.EmbeddedCacheManager;

@AcceptanceTest
public class CachesConfigurationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private EmbeddedCacheManager infinispanCacheManager;

    @Test
    public void testCacheManager() {

    //TODO

        System.out.println(">>>>>> ");
    }
}
