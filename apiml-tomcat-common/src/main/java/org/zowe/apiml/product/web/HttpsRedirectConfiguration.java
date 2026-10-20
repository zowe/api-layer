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
        String publicOrigin;
        try {
            publicOrigin = externalUrl.isBlank() ? null : HttpsRedirectEndpoint.normalizeOrigin(externalUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid apiml.service.externalUrl for HTTP redirect: expected an HTTPS "
                + "origin with a valid port and without user information, path, query or fragment", e);
        }
        try {
            HttpsRedirectEndpoint.origin(hostname, -1);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid apiml.service.hostname for HTTP redirect", e);
        }
        factory.setProtocol(HttpsRedirectProtocol.class.getName());
        factory.addConnectorCustomizers(connector -> connector.addLifecycleListener(event -> {
            if (Lifecycle.BEFORE_INIT_EVENT.equals(event.getType())
                && connector.getProtocolHandler() instanceof HttpsRedirectProtocol protocol) {
                // Additional connectors receive their final port AFTER Boot customizes them.
                if (connector.getPort() == factory.getPort() && publicOrigin != null) {
                    protocol.setHttpsRedirectOrigin(publicOrigin);
                } else {
                    // The endpoint resolves its actual bound port when accepting a connection, including port 0.
                    protocol.setHttpsRedirectHostname(hostname);
                }
            }
        }));
    }
}
