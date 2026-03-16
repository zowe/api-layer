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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.zaas.security.service.saf.SafIdtProvider;
import org.zowe.apiml.zaas.security.service.saf.SafRestAuthenticationService;

import javax.annotation.Nonnull;

@TestConfiguration
@Profile("OpenTelemetryTest")
public class OpenTelemetryTestConfig implements BeanPostProcessor {

    @Autowired
    @Lazy
    private LogRecordExporter logRecordExporter;

    @Bean
    InMemoryMetricReader inMemoryMetricReader() {
        return InMemoryMetricReader.create();
    }

    @Bean
    @Primary
    LogRecordExporter inMemoryLogRecordExporter() {
        return InMemoryLogRecordExporter.create();
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof OpenTelemetry otel) {
            var logBridge = otel.getLogsBridge();
            var delegate = ReflectionTestUtils.getField(logBridge, "delegate");
            ReflectionTestUtils.setField(delegate, "isNoopLogRecordProcessor", false);
            var sharedState = ReflectionTestUtils.getField(delegate, "sharedState");
            ReflectionTestUtils.setField(sharedState, "logRecordProcessor", SimpleLogRecordProcessor.builder(logRecordExporter).build());
            return otel;
        }

        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    @Bean
    SafIdtProvider safIdtProvider(RestTemplate restTemplate) {
        return new SafRestAuthenticationService(restTemplate);
    }

    @Bean
    AutoConfigurationCustomizerProvider otelCustomizer(@Nonnull InMemoryMetricReader reader) {
        return p -> p.addMeterProviderCustomizer((meterProviderBuilder, configProperties) ->
            meterProviderBuilder.registerMetricReader(reader));
    }

}
