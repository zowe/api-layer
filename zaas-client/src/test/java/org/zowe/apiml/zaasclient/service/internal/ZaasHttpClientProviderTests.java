/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.service.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ZaasHttpClientProviderTests {
    @Test
    void testHttpClientCanCreateHttpClient() {
        ZaasHttpClientProvider zaasHttpClientProvider = new ZaasHttpClientProvider();
        assertNotNull(zaasHttpClientProvider.getHttpClient());
    }
}
