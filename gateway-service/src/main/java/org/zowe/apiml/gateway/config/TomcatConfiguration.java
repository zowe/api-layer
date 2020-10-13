/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Configuration of Tomcat
 */
@Configuration
public class TomcatConfiguration {
    @Bean
    public ServletWebServerFactory servletContainer() {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.setProtocol(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        tomcat.addAdditionalTomcatConnectors(createSslConnector());
        return tomcat;
    }

    private Connector createSslConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();

        File keystore = new File("keystore/localhost/localhost.keystore.p12");
        File truststore = new File("keystore/localhost/localhost.truststore.p12");
        connector.setScheme("https");
        connector.setSecure(true);
        connector.setPort(55756);
        protocol.setSSLEnabled(true);
        protocol.setKeystoreFile(keystore.getAbsolutePath());
        protocol.setKeystorePass("password");
        protocol.setTruststoreFile(truststore.getAbsolutePath());
        protocol.setTruststorePass("password");
        protocol.setKeyAlias("localhost");
        return connector;
    }
}
