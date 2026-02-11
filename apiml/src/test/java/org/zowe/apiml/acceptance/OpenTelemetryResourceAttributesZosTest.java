/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.*;

@AcceptanceTest
@ActiveProfiles("OpenTelemetryTest")
@TestPropertySource(
    properties = {
        "otel.sdk.disabled=false",
        "otel.metrics.exporter=none",
        "otel.traces.exporter=none",
        "otel.logs.exporter=none"
    }
)
@DirtiesContext
class OpenTelemetryResourceAttributesZosTest {

    static {
        // Backup th original value so it can be restored in org.zowe.apiml.acceptance.TestConfig2.TestApimlZosOpenTelemetryResourceProvider.calculateAttributes
        System.setProperty("os.default.name", System.getProperty("os.name"));
        // Used to enable z/OS resource attribute bean
        System.setProperty("os.name", "z/OS");
    }

    @Autowired
    private InMemoryMetricReader metricReader;

    @LocalServerPort
    private int port;

    @Test
    void thenLogCustomAttributes() {
        var metrics = metricReader.collectAllMetrics();
        assertFalse(metrics.isEmpty(), "No data received");

        metrics.forEach(
            metric -> {
                var attributes = metric.getResource().getAttributes();
                assertEquals("zos", attributes.get(stringKey("os.type")));
                assertEquals("STC1111", attributes.get(stringKey("process.zos.jobid")));
                assertEquals("ZWE1AG", attributes.get(stringKey("process.zos.jobname")));
                assertEquals("apiml1:" + port, attributes.get(stringKey("service.name")));
                assertNull(attributes.get(stringKey("service.namespace")));
                assertNotNull(attributes.get(stringKey("service.version")));
                assertEquals("030200", attributes.get(stringKey("os.version")));
                assertEquals("LR10", attributes.get(stringKey("zos.smf.id")));
                assertEquals("localhost:gateway:" + port, attributes.get(stringKey("service.instance.id")));
            }
        );
    }

    @AfterAll
    static void cleanUp() {
        System.setProperty("os.name", System.getProperty("os.default.name"));
    }
}
