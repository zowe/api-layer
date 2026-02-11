/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.opentelemetry;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ApimlNonZosOpenTelemetryResourceProviderTest {

    private ApimlNonZosOpenTelemetryResourceProvider resourceProvider;

    @BeforeEach
    void setUp() {
        this.resourceProvider = new ApimlNonZosOpenTelemetryResourceProvider();
    }

    @Test
    void testCalculateAttributes() {
        var result = resourceProvider.calculateAttributes();
        assertFalse(result.isEmpty());
    }

    @Test
    void testCreateResource() {
        var result = resourceProvider.createResource(mock(ConfigProperties.class));
        assertFalse(result.getAttributes().isEmpty());
    }

}
