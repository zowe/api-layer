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

import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.annotation.Nonnull;

@TestConfiguration
@Profile("OpenTelemetryTest")
public class OpenTelemetryTestConfig {

    @Bean
    InMemoryMetricReader inMemoryMetricReader() {
        return InMemoryMetricReader.create();
    }

    @Bean
    @Primary
    LogRecordExporter inMemoryLogRecordExporter() {
        return InMemoryLogRecordExporter.create();
    }

    @SuppressWarnings("null")
    @Bean
    @Primary
    OpenTelemetrySdk openTelemetrySdk(ObjectProvider<SdkTracerProvider> tracerProvider,
            ObjectProvider<ContextPropagators> propagators, ObjectProvider<SdkLoggerProvider> loggerProvider,
            ObjectProvider<SdkMeterProvider> meterProvider,
            LogRecordExporter exporter) {
        OpenTelemetrySdkBuilder builder = OpenTelemetrySdk.builder();
        tracerProvider.ifAvailable(builder::setTracerProvider);
        propagators.ifAvailable(builder::setPropagators);
        var lp = SdkLoggerProvider.builder();
        lp.addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter));
        builder.setLoggerProvider(lp.build());
        meterProvider.ifAvailable(builder::setMeterProvider);
        return builder.build();
    }

    @Bean
    AutoConfigurationCustomizerProvider otelCustomizer(@Nonnull InMemoryMetricReader reader) {
        return p -> p.addMeterProviderCustomizer((meterProviderBuilder, configProperties) ->
            meterProviderBuilder.registerMetricReader(reader));
    }

}
