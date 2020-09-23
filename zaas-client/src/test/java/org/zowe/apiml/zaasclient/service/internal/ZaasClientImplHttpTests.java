package org.zowe.apiml.zaasclient.service.internal;
/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import org.junit.jupiter.api.Test;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.service.ZaasClient;

import static org.junit.Assert.assertNotNull;

class ZaasClientImplHttpTests {
    @Test
    void testHttpOnlyZaasClientCanBeCreated() throws ZaasConfigurationException {
        ConfigProperties configProperties = new ConfigProperties();
        configProperties.setHttpOnly(true);
        configProperties.setApimlHost("hostname");
        configProperties.setApimlPort("10010");
        configProperties.setApimlBaseUrl("/api/v1/gateway/auth");

        ZaasClient client = new ZaasClientImpl(configProperties);
        assertNotNull(client);
    }
}
