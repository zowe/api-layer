package org.zowe.apiml.acceptance;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@AcceptanceTest
@ActiveProfiles("OpenTelemetryTest")
@TestPropertySource(
    properties = {
        "otel.sdk.disabled=false",
        "otel.metrics.exporter=none",
        "otel.traces.exporter=none",
        "otel.logs.exporter=none",
    }
)
@Import(OpenTelemetryMetricsTest.TestConfig.class)
class OpenTelemetryMetricsTest {

    @Autowired
    private InMemoryMetricReader metricReader;

    @Test
    void testJvmMetrics() {
        Collection<MetricData> metrics = metricReader.collectAllMetrics();

        if (metrics.isEmpty()) {
            System.out.println("WARNING: No metrics found in reader.");
        } else {
            metrics.forEach(m -> System.out.println("Found metric: " + m.getName()));
        }

        assertThat(metrics).isNotEmpty();
    }

    @Profile("OpenTelemetryTest")
    @TestConfiguration
    static class TestConfig {
        @Bean
        public InMemoryMetricReader inMemoryMetricReader() {
            return InMemoryMetricReader.create();
        }

        @Bean
        public AutoConfigurationCustomizerProvider otelCustomizer(InMemoryMetricReader reader) {
            return p -> p.addMeterProviderCustomizer((meterProviderBuilder, configProperties) ->
                meterProviderBuilder.registerMetricReader(reader));
        }
    }
}
