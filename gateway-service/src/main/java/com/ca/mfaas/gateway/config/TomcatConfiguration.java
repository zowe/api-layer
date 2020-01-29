/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration of Tomcat
 */
@Configuration
public class TomcatConfiguration {

    @Value("${apiml.service.allowEncodedCharacters}")
    Boolean allowEncodedCharacters;

    @Bean
    public ServletWebServerFactory servletContainer() {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", getAllowEncodedCharacters());
        System.setProperty("org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH", getAllowEncodedCharacters());
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.setProtocol(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        return tomcat;
    }

    private String getAllowEncodedCharacters() {
        return allowEncodedCharacters != null ? allowEncodedCharacters.toString() : "false";
    }
}
