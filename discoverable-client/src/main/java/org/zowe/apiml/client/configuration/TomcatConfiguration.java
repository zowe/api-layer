/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.configuration;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration of Tomcat
 */
@Configuration
public class TomcatConfiguration {

    @Bean
    public ServletWebServerFactory servletContainer(List<TomcatConnectorCustomizer> connectorCustomizers) {
        connectorCustomizers.add(new UrlTomcatCustomizer());
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.setProtocol(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        tomcat.addConnectorCustomizers(connectorCustomizers.toArray(new TomcatConnectorCustomizer[0]));
        return tomcat;
    }

    static class UrlTomcatCustomizer implements TomcatConnectorCustomizer {

        @Override
        public void customize(Connector connector) {
            connector.setAllowBackslash(true);
            connector.setEncodedSolidusHandling(EncodedSolidusHandling.PASS_THROUGH.getValue());
        }

    }
}
