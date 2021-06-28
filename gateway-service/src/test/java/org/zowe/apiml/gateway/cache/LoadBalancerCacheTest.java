/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LoadBalancerCacheTest {
    @Nested
    class GivenEmptyCache {
        LoadBalancerCache cache = new LoadBalancerCache();

        @Nested
        class WhenItemStoredInCache {
            String user = "USER";
            String service = "DISCOVERABLECLIENT";
            String instance = "discoverable-client:discoverableclient:10012";

            @BeforeEach
            void store() {
                cache.store(user, service, instance);
            }

            @Test
            void itemIsRetrieved() {
                assertThat(cache.retrieve(user, service), is(instance));
            }
        }
    }
}
