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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.HttpsFactory;

@Configuration
@Import(value = OpenTelemetryImportSelector.class)
@DependsOn("httpConfig")
@Slf4j
public class ApimlOpenTelemetryConfiguration implements InitializingBean, BeanPostProcessor {

    static {
        System.setProperty("io.opentelemetry.exporter.internal.http.HttpSenderProvider", "org.zowe.apiml.product.opentelemetry.ApimlSenderProvider");
        System.setProperty("io.opentelemetry.exporter.internal.grcp.GrcpSenderProvider", "org.zowe.apiml.product.opentelemetry.ApimlSenderProvider");
    }

    @Autowired
    private HttpConfig httpConfig;

    private static HttpConfig httpConfig2;

    static HttpsFactory httpsConfig() {
        if (httpConfig2 == null) {
            return null;
        }
        return httpConfig2.httpsFactory();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ApimlOpenTelemetryConfiguration.httpConfig2 = httpConfig;
    }

}
