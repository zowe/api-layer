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

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
    InMemoryLogRecordExporter inMemoryLogRecordExporter() {
        return InMemoryLogRecordExporter.create();
    }

    @Bean
    AutoConfigurationCustomizerProvider otelCustomizer(@Nonnull InMemoryMetricReader reader) {
        return p -> p.addMeterProviderCustomizer((meterProviderBuilder, configProperties) ->
            meterProviderBuilder.registerMetricReader(reader));
    }

}
