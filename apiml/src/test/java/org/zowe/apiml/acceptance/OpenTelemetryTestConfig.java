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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.zowe.apiml.product.opentelemetry.ApimlOpenTelemetryResourceProvider;
import org.zowe.apiml.product.opentelemetry.ApimlZosOpenTelemetryResourceProvider;
import org.zowe.apiml.product.zos.ZosSystemInformation;

import javax.annotation.Nonnull;

@TestConfiguration
@Profile("OpenTelemetryTest")
public class OpenTelemetryTestConfig {

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
        @ConditionalOnProperty(name = "os.name", havingValue = "z/OS")
        ApimlOpenTelemetryResourceProvider apimlOpenTelemetryResourceProvider(ZosSystemInformation zosSystemInformation) {
            return new TestApimlZosOpenTelemetryResourceProvider(zosSystemInformation);
        }

        static class TestApimlZosOpenTelemetryResourceProvider extends ApimlZosOpenTelemetryResourceProvider {

            public TestApimlZosOpenTelemetryResourceProvider(ZosSystemInformation zosSystemInformation) {
                super(zosSystemInformation);
            }

            @Override
            public Attributes calculateAttributes() {
                var attributes = super.calculateAttributes();
                // Restore os.name to test runner's platform to avoid issues with classes that are not available on z/OS (for instance mockito fails)
                System.setProperty("os.name", System.getProperty("os.default.name"));
                return attributes;
            }
        }
}
