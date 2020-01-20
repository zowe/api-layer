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
import org.junit.Assert;
import org.junit.Test;

public class ApiDocKeyTest {

    @Test
    public void testCreationOfObjectAndValuesSet() {
        ApiDocCacheKey apiDocCacheKey = new ApiDocCacheKey("service", "1.0.0");
        Assert.assertNotNull(apiDocCacheKey);
        Assert.assertEquals("service", apiDocCacheKey.getServiceId());
        Assert.assertEquals("1.0.0", apiDocCacheKey.getApiVersion());
        apiDocCacheKey.setApiVersion("2.0.0");
        apiDocCacheKey.setServiceId("service1");
        Assert.assertEquals("service1", apiDocCacheKey.getServiceId());
        Assert.assertEquals("2.0.0", apiDocCacheKey.getApiVersion());
    }

    @Test
    public void testEqualsAndHasCode() {
        ApiDocCacheKey apiDocCacheKey = new ApiDocCacheKey("service", "1.0.0");
        ApiDocCacheKey apiDocCacheKey1 = new ApiDocCacheKey("service1", "2.0.0");
        Assert.assertNotEquals(apiDocCacheKey, apiDocCacheKey1);
        Assert.assertNotEquals(apiDocCacheKey.hashCode(), apiDocCacheKey1.hashCode());
    }
}
