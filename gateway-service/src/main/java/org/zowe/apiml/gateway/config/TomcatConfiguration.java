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
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${server.internal.enabled:false}")
    private boolean enableInternalPort;
    @Value("${server.internal.ssl.enabled:true}")
    private boolean enableSslOnInternal;
    @Value("${server.internal.ssl.clientAuth:want}")
    private String clientAuth;
    @Value("${server.internal.port:10017}")
    private int internalPort;

    @Value("${server.internal.ssl.keyStore:keystore/localhost/localhost.keystore.p12}")
    private String keyStorePath;
    @Value("${server.internal.ssl.keyStorePassword:password}")
    private String keyStorePassword;
    @Value("${server.internal.ssl.keyStoreType:PKCS12}")
    private String keyStoreType;

    @Value("${server.internal.ssl.keyPassword:password}")
    private String keyPassword;
    @Value("${server.internal.ssl.keyAlias:localhost}")
    private String keyAlias;

    @Value("${server.internal.ssl.trustStore:keystore/localhost/localhost.truststore.p12}")
    private String trustStorePath;
    @Value("${server.internal.ssl.trustStorePassword:password}")
    private String trustStorePassword;
    @Value("${server.internal.ssl.trustStoreType:PKCS12}")
    private String trustStoreType;
    @Value("${server.ssl.ciphers}")
    private String ciphers;

    @Bean
    public ServletWebServerFactory servletContainer() {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.setProtocol(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        if (enableInternalPort) {
            tomcat.addAdditionalTomcatConnectors(createSslConnector());
        }
        return tomcat;
    }

    private Connector createSslConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();

        connector.setPort(internalPort);
        if (enableSslOnInternal) {
            connector.setScheme("https");

            connector.setSecure(true);
            protocol.setSSLEnabled(true);
            protocol.setSslEnabledProtocols("TLSv1.2");
            protocol.setSSLHonorCipherOrder(true);
            protocol.setCiphers(ciphers);
            protocol.setClientAuth(clientAuth);

            File keyStore = new File(keyStorePath);
            File trustStore = new File(trustStorePath);

            protocol.setKeystoreFile(keyStore.getAbsolutePath());
            protocol.setKeystorePass(keyStorePassword);
            protocol.setKeystoreType(keyStoreType);
            protocol.setTruststoreFile(trustStore.getAbsolutePath());
            protocol.setTruststorePass(trustStorePassword);
            protocol.setTruststoreType(trustStoreType);
            protocol.setKeyAlias(keyAlias);
            protocol.setKeyPass(keyPassword);
        } else {
            connector.setScheme("http");
            connector.setSecure(false);
        }

        return connector;
    }
}
