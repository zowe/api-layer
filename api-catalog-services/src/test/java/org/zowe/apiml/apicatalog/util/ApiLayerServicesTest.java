/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.zowe.apiml.apicatalog.config.ApiLayerServices;
import static org.junit.jupiter.api.Assertions.*;

public class ApiLayerServicesTest {

    @Test
    public void testEnumConstruction_shouldHaveCorrectServiceIds() {
        assertEquals("discovery", ApiLayerServices.DISCOVERY.getServiceId());
        assertEquals("gateway", ApiLayerServices.GATEWAY.getServiceId());
        assertEquals("apiml", ApiLayerServices.APIML.getServiceId());
        assertEquals("zaas", ApiLayerServices.ZAAS.getServiceId());
        assertEquals("apicatalog", ApiLayerServices.API_CATALOG.getServiceId());
        assertEquals("cachingservice", ApiLayerServices.CACHING_SERVICE.getServiceId());
    }

    @Test
    public void testToString_shouldReturnEnumConstantName() {
        assertEquals("DISCOVERY", ApiLayerServices.DISCOVERY.toString());
        assertEquals("GATEWAY", ApiLayerServices.GATEWAY.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"discovery", "DISCOVERY", "Discovery", " discovery ", "DiScOvErY"})
    public void testIsApiLayerService_withDiscoveryVariants_shouldReturnTrue(String input) {
        boolean result = ApiLayerServices.isApiLayerService(input);
        assertTrue(result, "Should recognize '" + input + "' as an API Layer service");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    public void testIsApiLayerService_withNullOrEmptyOrBlank_shouldReturnFalse(String input) {
        boolean result = ApiLayerServices.isApiLayerService(input);
        assertFalse(result, "Should not recognize null or empty string as an API Layer service");
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "discoveryservice", "gateways", "api", "catalog", "caching", "notaservice", "external-service"})
    public void testIsApiLayerService_withNonApiLayerServices_shouldReturnFalse(String input) {
        boolean result = ApiLayerServices.isApiLayerService(input);
        assertFalse(result, "Should not recognize '" + input + "' as an API Layer service");
    }
}
