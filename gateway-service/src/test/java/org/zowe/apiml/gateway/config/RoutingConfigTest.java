/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutingConfigTest {

    private static final String CONDITIONAL_REMOVAL = "RemoveRequestHeaderIfNotCrossSite";
    private static final String UNCONDITIONAL_REMOVAL = "RemoveRequestHeader";

    private List<FilterDefinition> commonNoRetryFilters(boolean preserveOriginForCrossSite) {
        RoutingConfig routingConfig = new RoutingConfig();
        ReflectionTestUtils.setField(routingConfig, "ignoredHeadersWhenCorsEnabled", "Origin");
        ReflectionTestUtils.setField(routingConfig, "acceptForwardedCert", false);
        ReflectionTestUtils.setField(routingConfig, "allowEncodedSlashes", true);
        ReflectionTestUtils.setField(routingConfig, "preserveOriginForCrossSite", preserveOriginForCrossSite);
        return routingConfig.commonNoRetryFilters();
    }

    private boolean hasFilterNamed(List<FilterDefinition> filters, String name) {
        return filters.stream().anyMatch(filter -> name.equals(filter.getName()));
    }

    @Test
    void givenPreserveOriginEnabled_thenConditionalRemovalFilterIsUsed() {
        List<FilterDefinition> filters = commonNoRetryFilters(true);
        assertTrue(hasFilterNamed(filters, CONDITIONAL_REMOVAL));
        assertFalse(hasFilterNamed(filters, UNCONDITIONAL_REMOVAL));
    }

    @Test
    void givenPreserveOriginDisabled_thenUnconditionalRemovalFilterIsUsed() {
        List<FilterDefinition> filters = commonNoRetryFilters(false);
        assertTrue(hasFilterNamed(filters, UNCONDITIONAL_REMOVAL));
        assertFalse(hasFilterNamed(filters, CONDITIONAL_REMOVAL));
    }

}
