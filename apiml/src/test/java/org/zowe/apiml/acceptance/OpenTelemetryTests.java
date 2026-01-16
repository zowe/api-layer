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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenTelemetryTests {

    @Nested
    @AcceptanceTest
    @ActiveProfiles("OpenTelemetryTest")
    class TestClass {

        @Autowired
        private InMemorySpanExporter inMemorySpanExporter;

        @Test
        void testSomething() {
            // Startup should generate something already


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

}
