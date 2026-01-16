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

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenTelemetryTests {

    @Nested
    @AcceptanceTest
    @ActiveProfiles("OpenTelemetryTest")
    @TestPropertySource(
        properties = {
            "otel.sdk.disabled=false"
        }
    )
    class TestClass {

        @Autowired
        private InMemorySpanExporter inMemorySpanExporter;

        @SuppressWarnings("null")
        @Test
        void testSomething() {
            // Startup should generate something already

            var result = inMemorySpanExporter.flush();
            assertTrue(result.isSuccess());

            var data = inMemorySpanExporter.getFinishedSpanItems();
            assertFalse(data.isEmpty(), "No data collected");
            assertTrue(data.stream()
                .anyMatch(d -> {
                    var attributes = d.getAttributes();
                    assertEquals("JOB1111", attributes.get(stringKey(null)));
                    assertEquals(d, data);
                    return true;
                }

            ), "No data matches");

        }

    }

}

@TestConfiguration
@Profile("OpenTelemetryTest")
class OpenTelemetryConfiguration {

    @Bean
    @Primary
    SpanExporter spanExporter() {
        return InMemorySpanExporter.create();
    }

    @Bean
    InMemorySpanExporter inMemorySpanExporter(SpanExporter spanExporter) {
        return (InMemorySpanExporter) spanExporter;
    }

}
