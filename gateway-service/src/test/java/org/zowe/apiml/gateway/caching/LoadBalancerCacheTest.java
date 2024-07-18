/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.caching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoadBalancerCacheTest {

    @Mock
    private CachingServiceClient cachingServiceClient;

    @InjectMocks
    private LoadBalancerCache loadBalancerCache;

    @BeforeEach
    void setUp() {

    }

    @Nested
    class GivenLoadBalancerCache {

        @Nested
        class WhenCreate {

        }

        @Nested
        class WhenDelete {

        }

        @Nested
        class WhenUpdate {

        }

        @Nested
        class WhenRetrieve {

        }

    }

}
