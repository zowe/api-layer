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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.product.zos.ZosSystemInformation;

import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class ApimlZosOpenTelemetryResourceProviderTest {

    @Mock
    private ZosSystemInformation zosSystemInformation;

    private ApimlZosOpenTelemetryResourceProvider resourceProvider;

    @BeforeEach
    void setUp() {
        resourceProvider = new ApimlZosOpenTelemetryResourceProvider(zosSystemInformation);
    }

    @Nested
    class GivenZosAttributes {

        @Test
        void testCalculateAttributes() {
            var attributes = resourceProvider.calculateAttributes();

            assertFalse(attributes.isEmpty());
        }

    }

}
