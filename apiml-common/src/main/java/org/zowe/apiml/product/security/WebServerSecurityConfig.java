/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Configuration of web server security
 */
@Slf4j
@Configuration
@ConditionalOnMissingBean(name = "modulithConfig")
public class WebServerSecurityConfig {

    @Bean
    public TomcatConnectorCustomizer servletContainerCustomizer() {
        return connector -> {
            AbstractHttp11Protocol<?> abstractProtocol = (AbstractHttp11Protocol<?>) connector.getProtocolHandler();
            Arrays.stream(abstractProtocol.findSslHostConfigs()).forEach(sslHost -> sslHost.setHonorCipherOrder(true));
            log.debug("servletContainerCustomizer initialized");
        };
    }

}
