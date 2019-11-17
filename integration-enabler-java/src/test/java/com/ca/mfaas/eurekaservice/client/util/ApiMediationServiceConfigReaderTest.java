/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.util;

import com.ca.mfaas.eurekaservice.client.config.ApiMediationServiceConfig;
import com.ca.mfaas.eurekaservice.client.config.Route;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApiMediationServiceConfigReaderTest {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void readConfiguration() {
        String file = "/service-configuration.yml";

        ApiMediationServiceConfig result = new ApiMediationServiceConfigReader().readConfigurationFile(file);

        assertTrue(result.getDiscoveryServiceUrls().contains("http://eureka:password@localhost:10011/eureka"));
        assertEquals("service", result.getServiceId());
        assertEquals("/", result.getHomePageRelativeUrl());
        assertEquals("/application/info", result.getStatusPageRelativeUrl());
        assertEquals("/application/health", result.getHealthCheckRelativeUrl());
        assertTrue(result.getRoutes().contains(new Route("api/v1/api-doc", "/hellospring/api-doc")));
        assertTrue(result.getCatalog().getTile().getVersion().equals("1.0.0"));
    }

    @Test
    public void readNotExistingConfiguration() {
        String file = "no-existing-file";
/*
        exceptionRule.expect(ApiMediationServiceConfigReaderException.class);
        exceptionRule.expectMessage(String.format("File [%s] doesn't exist", file));
*/

        assertNull(new ApiMediationServiceConfigReader().readConfigurationFile(file));

    }

    @Test
    public void readConfigurationWithWrongFormat() {
        String file = "/bad-format-of-service-configuration.yml";
        exceptionRule.expect(ApiMediationServiceConfigReaderException.class);
        exceptionRule.expectMessage(String.format("File [%s] can't be parsed as ApiMediationServiceConfig", file.substring(1)));

        new ApiMediationServiceConfigReader().readConfigurationFile(file);
    }
}
