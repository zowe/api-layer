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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.util.unit.DataSize;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomDiskSpaceHealthIndicatorTest {

    private File testPath;
    private DataSize testThreshold;
    private CustomDiskSpaceHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        testPath = new File(System.getProperty("java.io.tmpdir"));
        testThreshold = DataSize.ofMegabytes(10);
        healthIndicator = new CustomDiskSpaceHealthIndicator(testPath, testThreshold);
    }

    @Nested
    class WhenCheckingHealth {
        @Test
        void shouldAlwaysReportUpStatus() {

            Health.Builder builder = new Health.Builder();
            // When
            healthIndicator.doHealthCheck(builder);
            Health health = builder.build();
            // Then
            assertEquals(Status.UP, health.getStatus());
        }

        @Test
        void shouldIncludeExpectedDetails() {

            Health.Builder builder = new Health.Builder();
            // When
            healthIndicator.doHealthCheck(builder);
            Health health = builder.build();
            // Then
            assertEquals("not monitored", health.getDetails().get("total"));
            assertEquals("not monitored", health.getDetails().get("free"));
            assertEquals(testThreshold.toBytes(), health.getDetails().get("threshold"));
            assertEquals(testPath.getAbsolutePath(), health.getDetails().get("path"));
            assertEquals(testPath.exists(), health.getDetails().get("exists"));
            assertEquals("Disk space monitoring disabled", health.getDetails().get("note"));
        }
    }
}
