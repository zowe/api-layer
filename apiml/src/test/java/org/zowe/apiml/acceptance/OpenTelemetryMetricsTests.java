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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.product.opentelemetry.ApimlOpenTelemetryResourceProvider;
import org.zowe.apiml.product.opentelemetry.ApimlZosOpenTelemetryResourceProvider;
import org.zowe.apiml.product.zos.ZosSystemInformation;

import javax.annotation.Nonnull;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenTelemetryMetricsTest {

    @Nested
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
    @TestInstance(Lifecycle.PER_CLASS)
    @DirtiesContext
    class WhenOpenTelemetryEnabled {

        private static String defaultPlatform = System.getProperty("os.name");

        static {
            // Used to enable z/OS resource attribute bean
            System.setProperty("os.name", "z/OS");
        }

        @Autowired
        private InMemoryMetricReader metricReader;

        @BeforeAll
        void init() {
            System.setProperty("os.name", defaultPlatform);
        }

        @Test
        void whenZos_thenLogCustomAttributes() {
            var metrics = metricReader.collectAllMetrics();
            assertFalse(metrics.isEmpty(), "No data received");

            metrics.forEach(
                metric -> {
                    var attributes = metric.getResource().getAttributes();
                    assertEquals("zos", attributes.get(stringKey("os.type")));
                    assertEquals("STC1111", attributes.get(stringKey("process.zos.jobid")));
                    assertEquals("ZWE1AG", attributes.get(stringKey("process.zos.jobname")));
                    assertEquals("gateway", attributes.get(stringKey("service.name")));
                    assertEquals("apiml:apiml1:40985", attributes.get(stringKey("service.namespace")));
                    assertNotNull(attributes.get(stringKey("service.version")));
                }
            );

        }

        @Profile("OpenTelemetryTest")
        @TestConfiguration
        static class TestConfig {

            @Bean
            InMemoryMetricReader inMemoryMetricReader() {
                return InMemoryMetricReader.create();
            }

            @Bean
            AutoConfigurationCustomizerProvider otelCustomizer(@Nonnull InMemoryMetricReader reader) {
                return p -> p.addMeterProviderCustomizer((meterProviderBuilder, configProperties) ->
                    meterProviderBuilder.registerMetricReader(reader));
            }

            @Bean
            @Primary
            ApimlOpenTelemetryResourceProvider apimlOpenTelemetryResourceProvider(ZosSystemInformation zosSystemInformation) {
                return new TestApimlZosOpenTelemetryResourceProvider(zosSystemInformation);
            }

        }

        static class TestApimlZosOpenTelemetryResourceProvider extends ApimlZosOpenTelemetryResourceProvider {

            public TestApimlZosOpenTelemetryResourceProvider(ZosSystemInformation zosSystemInformation) {
                super(zosSystemInformation);
            }

            @Override
            public Attributes calculateAttributes() {
                var attributes = super.calculateAttributes();
                // Restore os.name to test runner's platform to avoid issues with classes that are not available on z/OS (for instance mockito fails)
                System.setProperty("os.name", defaultPlatform);
                return attributes;
            }

        }

    }

}
