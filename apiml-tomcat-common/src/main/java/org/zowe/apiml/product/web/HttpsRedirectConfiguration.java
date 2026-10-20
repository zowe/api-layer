/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.web;

import org.apache.catalina.Lifecycle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.tomcat.TomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "apiml.security.httpRedirect.enabled", havingValue = "true")
public class HttpsRedirectConfiguration implements WebServerFactoryCustomizer<TomcatWebServerFactory>, Ordered {

    private final boolean nativeTls;
    private final String externalUrl;
    private final String hostname;

    public HttpsRedirectConfiguration(
        @Value("${server.ssl.enabled:true}") boolean sslEnabled,
        @Value("${server.attlsServer.enabled:false}") boolean attlsEnabled,
        @Value("${apiml.service.externalUrl:}") String externalUrl,
        @Value("${apiml.service.hostname:localhost}") String hostname
    ) {
        nativeTls = sslEnabled && !attlsEnabled;
        this.externalUrl = externalUrl;
        this.hostname = hostname;
    }

    @Override
    public int getOrder() {
        // Select the protocol before additional connectors are created.
        return -1;
    }

    @Override
    public void customize(TomcatWebServerFactory factory) {
        if (!nativeTls) {
            return;
        }
        factory.setProtocol(HttpsRedirectProtocol.class.getName());
        factory.addConnectorCustomizers(connector -> connector.addLifecycleListener(event -> {
            if (Lifecycle.BEFORE_INIT_EVENT.equals(event.getType())
                && connector.getProtocolHandler() instanceof HttpsRedirectProtocol protocol) {
                // Additional connectors receive their final port AFTER Boot customizes them.
                String origin = connector.getPort() == factory.getPort() && !externalUrl.isBlank()
                    ? externalUrl : origin(hostname, connector.getPort());
                protocol.setHttpsRedirectOrigin(origin);
                protocol.setMaxHttpRequestHeaderSize(protocol.getMaxHttpRequestHeaderSize());
            }
        }));
    }

    private static String origin(String hostname, int port) {
        try {
            return new URI("https", null, hostname, port, null, null, null).toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid HTTPS redirect hostname", e);
        }
    }
}
