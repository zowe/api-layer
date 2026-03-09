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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@AcceptanceTest
@ActiveProfiles({ "OpenTelemetryTest" })
@TestPropertySource(
    properties = {
        "otel.sdk.disabled=false",
        "otel.metrics.exporter=none",
        "otel.traces.exporter=none",
        "otel.logs.exporter=none"
    }
)
@DirtiesContext
class OpenTelemetryResourceAttributesNonZosTest {

    @Autowired
    private InMemoryMetricReader metricReader;

    @LocalServerPort
    private int port;

    private void assertLogBase() {

    }

    @Test
    void thenLogCustomAttributes() {
        var metrics = metricReader.collectAllMetrics();
        assertFalse(metrics.isEmpty(), "No data received");

        metrics.forEach(
            metric -> {
                var attributes = metric.getResource().getAttributes();
                assertNotEquals("zos", attributes.get(stringKey("os.type")));
                assertNull(attributes.get(stringKey("process.zos.jobid")));
                assertNull(attributes.get(stringKey("process.zos.jobname")));
                assertEquals("apiml:apiml1:" + port, attributes.get(stringKey("service.name")));
                assertNull(attributes.get(stringKey("service.namespace")));
                assertNotNull(attributes.get(stringKey("service.version")));
                assertNull(attributes.get(stringKey("zos.smf.id")));
                assertEquals("localhost:gateway:" + port, attributes.get(stringKey("service.instance.id")));
                assertNull(attributes.get(stringKey("deployment.environment.name")));
            }
        );
    }

}
