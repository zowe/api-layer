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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@AcceptanceTest
@ActiveProfiles({ "OpenTelemetryTest", "zos" })
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
                assertEquals("ZWE1AG", attributes.get(stringKey("process.zos.jobname")));
                assertEquals("apiml:apiml1:" + port, attributes.get(stringKey("service.name")));
                assertNull(attributes.get(stringKey("service.namespace")));
                assertNotNull(attributes.get(stringKey("service.version")));
                assertNotNull(attributes.get(stringKey("os.version")));
                assertEquals("LR10", attributes.get(stringKey("zos.smf.id")));
                assertEquals("localhost:gateway:" + port, attributes.get(stringKey("service.instance.id")));
                assertEquals("DEV", attributes.get(stringKey("deployment.environment.name")));
            }
        );
    }
}
