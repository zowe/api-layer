/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.services;

import org.zowe.apiml.apicatalog.services.cached.model.ApiDocCacheKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ApiDocKeyTest {

    @Test
    void testCreationOfObjectAndValuesSet() {
        ApiDocCacheKey apiDocCacheKey = new ApiDocCacheKey("service", "1.0.0");
        Assertions.assertNotNull(apiDocCacheKey);
        Assertions.assertEquals("service", apiDocCacheKey.getServiceId());
        Assertions.assertEquals("1.0.0", apiDocCacheKey.getApiVersion());
        apiDocCacheKey.setApiVersion("2.0.0");
        apiDocCacheKey.setServiceId("service1");
        Assertions.assertEquals("service1", apiDocCacheKey.getServiceId());
        Assertions.assertEquals("2.0.0", apiDocCacheKey.getApiVersion());
    }

    @Test
    void testEqualsAndHasCode() {
        ApiDocCacheKey apiDocCacheKey = new ApiDocCacheKey("service", "1.0.0");
        ApiDocCacheKey apiDocCacheKey1 = new ApiDocCacheKey("service1", "2.0.0");
        Assertions.assertNotEquals(apiDocCacheKey, apiDocCacheKey1);
        Assertions.assertNotEquals(apiDocCacheKey.hashCode(), apiDocCacheKey1.hashCode());
    }
}
