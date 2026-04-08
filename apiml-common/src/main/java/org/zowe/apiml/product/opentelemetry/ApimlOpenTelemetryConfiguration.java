/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.opentelemetry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.HttpsFactory;

/**
 * The goal of this configuration class is to rely on the default OpenTelemetry configuration from
 * OpenTelemetry's Spring Boot starter which in turn uses Java SPI for loading implementations
 * but making sure httpConfig bean is created before OpenTelemetry's configuration creates the HTTP clients for the exporter.
 */
@Configuration
@Import(value = OpenTelemetryImportSelector.class)
@DependsOn("httpConfig")
@Slf4j
@RequiredArgsConstructor
public class ApimlOpenTelemetryConfiguration implements InitializingBean, BeanPostProcessor {

    static {
        System.setProperty("io.opentelemetry.exporter.internal.http.HttpSenderProvider", "org.zowe.apiml.product.opentelemetry.ApimlSenderProvider");
        System.setProperty("io.opentelemetry.exporter.internal.grcp.GrcpSenderProvider", "org.zowe.apiml.product.opentelemetry.ApimlSenderProvider");
    }

    private final HttpConfig httpConfig;

    private static HttpConfig initializedHttpConfig;

    static HttpsFactory httpsConfig() {
        if (initializedHttpConfig == null) {
            return null;
        }
        return initializedHttpConfig.httpsFactory();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ApimlOpenTelemetryConfiguration.initializedHttpConfig = httpConfig; // NOSONAR
    }

}
