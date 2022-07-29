/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.product.web.ApimlTomcatCustomizer;

@Configuration
@Data
@ToString
public class GeneralConfig {
    @Value("${caching.storage.evictionStrategy:reject}")
    private String evictionStrategy;
    @Value("${caching.storage.size:100}")
    private int maxDataSize;

    @Bean
    @ConditionalOnProperty(name = "server.attls.enabled", havingValue = "true")
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return new ApimlTomcatCustomizer<>();
    }
}
