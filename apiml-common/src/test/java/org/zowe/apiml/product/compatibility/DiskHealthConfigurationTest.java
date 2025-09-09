/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.compatibility;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthIndicatorProperties;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.util.unit.DataSize;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class DiskHealthConfigurationTest {

    @Nested
    class WhenCreatingDiskSpaceHealthIndicator {
        @Test
        void shouldReturnCustomImplementation() {

            DiskHealthConfiguration configuration = new DiskHealthConfiguration();
            DiskSpaceHealthIndicatorProperties properties = new DiskSpaceHealthIndicatorProperties();
            properties.setPath(new File(System.getProperty("java.io.tmpdir")));
            properties.setThreshold(DataSize.ofMegabytes(100));

            // When
            DiskSpaceHealthIndicator indicator = configuration.diskSpaceHealthIndicator(properties);

            // Then
            assertNotNull(indicator);
            assertTrue(indicator instanceof CustomDiskSpaceHealthIndicator);
        }
    }
}
