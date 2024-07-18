/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.standalone;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zowe.apiml.product.instance.ServiceAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = ApiDocTransformForMock.class, properties = {
    "apiml.catalog.standalone.enabled=true",
    "server.hostname=host",
    "server.port=123",
    "service.schema=http"
})
class ApiDocTransformForMockTest {

    @Autowired
    private ServiceAddress gatewayConfigProperties;

    @Test
    void gatewayConfigPropertiesForMock() {
        assertNotNull(gatewayConfigProperties);
        assertEquals("host:123/apicatalog/mock", gatewayConfigProperties.getHostname());
        assertEquals("http", gatewayConfigProperties.getScheme());
    }

}