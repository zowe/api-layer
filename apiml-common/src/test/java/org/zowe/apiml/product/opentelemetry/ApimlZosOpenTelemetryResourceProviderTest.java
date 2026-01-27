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
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.product.zos.ZosSystemInformation;

import java.util.Map;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApimlZosOpenTelemetryResourceProviderTest {

    @Mock
    private ZosSystemInformation zosSystemInformation;

    private ApimlZosOpenTelemetryResourceProvider resourceProvider;

    @BeforeEach
    void setUp() {
        resourceProvider = new ApimlZosOpenTelemetryResourceProvider(zosSystemInformation);
        ReflectionTestUtils.setField(resourceProvider, "port", 10010);
    }

    @Nested
    class GivenZosAttributes {

        @Test
        void testCalculateAttributes() {
            when(zosSystemInformation.get()).thenReturn(Map.of(
                "zos.jobid", "JOB12345",
                "zos.jobname", "JOBN12",
                "zos.userid", "ZWEUSR",
                "zos.pid", 123456,
                "zos.sysname", "SYSA",
                "zos.sysclone", "16",
                "zos.sysplex", "PLEX1"
            ));
            var attributes = resourceProvider.calculateAttributes();

            assertFalse(attributes.isEmpty());
            assertNull(attributes.get(stringKey("mainframe.lpar.name")));

            assertEquals("JOB12345", attributes.get(stringKey("process.zos.jobid")));
            assertEquals("JOBN12", attributes.get(stringKey("process.zos.jobname")));
            assertEquals("ZWEUSR", attributes.get(stringKey("process.zos.userid")));
            assertEquals("apiml:PLEX1:10010", attributes.get(stringKey("service.namespace")));
            assertEquals("PLEX1:10010", attributes.get(stringKey("service.name")));
        }

    }

}
