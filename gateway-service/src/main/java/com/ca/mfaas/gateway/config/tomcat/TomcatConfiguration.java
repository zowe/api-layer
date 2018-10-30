/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.config.tomcat;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientConnectionManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;


/**
 * Configuration of Tomcat for the API Gateway.
 */
@Configuration
@Slf4j
public class TomcatConfiguration {

    @Bean
    public ApacheHttpClientConnectionManagerFactory connManFactory() {
        return new DefaultApacheHttpClientConnectionManagerFactory() {
            @Override
            public HttpClientConnectionManager newConnectionManager(boolean disableSslValidation, int maxTotalConnections, int maxConnectionsPerRoute,
                                                                    long timeToLive, TimeUnit timeUnit, RegistryBuilder registryBuilder) {

                final RegistryBuilder<ConnectionSocketFactory> registryBuilderToUse;
                if (registryBuilder == null) {
                    registryBuilderToUse =
                        RegistryBuilder.<ConnectionSocketFactory>create()
                            .register(HTTP_SCHEME, PlainConnectionSocketFactory.INSTANCE)
                            .register(HTTPS_SCHEME, SSLConnectionSocketFactory.getSocketFactory());
                } else {
                    registryBuilderToUse = registryBuilder;
                }

                return super.newConnectionManager(disableSslValidation, maxTotalConnections, maxConnectionsPerRoute, timeToLive, timeUnit,
                    registryBuilderToUse);
            }
        };
    }

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.setProtocol(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        return tomcat;
    }
}
