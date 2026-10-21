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

import org.apache.tomcat.util.net.TLSClientHelloExtractor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.tomcat.TomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Replaces Tomcat's plaintext-on-TLS error response with a fixed HTTPS redirect.
 * The response is shared by all connectors using the same Tomcat class loader.
 * Tomcat retains its normal TLS handshake, HTTP detection and connection closing behavior.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "apiml.security.httpRedirect.enabled", havingValue = "true")
public class HttpsRedirectConfiguration implements WebServerFactoryCustomizer<TomcatWebServerFactory>, DisposableBean {

    private final boolean nativeTls;
    private final String externalUrl;
    private byte[] previousResponse;
    private byte[] redirectResponse;

    public HttpsRedirectConfiguration(
        @Value("${server.ssl.enabled:true}") boolean sslEnabled,
        @Value("${server.attlsServer.enabled:false}") boolean attlsEnabled,
        @Value("${apiml.service.externalUrl:}") String externalUrl
    ) {
        nativeTls = sslEnabled && !attlsEnabled;
        this.externalUrl = externalUrl;
    }

    @Override
    public void customize(TomcatWebServerFactory factory) {
        if (!nativeTls) {
            return;
        }
        String location = redirectLocation(externalUrl);
        synchronized (TLSClientHelloExtractor.class) {
            if (redirectResponse == null) {
                redirectResponse = ("HTTP/1.1 308 Permanent Redirect\r\n"
                    + "Location: " + location + "\r\n"
                    + "Content-Length: 0\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII);
                previousResponse = TLSClientHelloExtractor.USE_TLS_RESPONSE;
                TLSClientHelloExtractor.USE_TLS_RESPONSE = redirectResponse;
            }
        }
    }

    private static String redirectLocation(String externalUrl) {
        URI destination = URI.create(externalUrl);
        if (!"https".equalsIgnoreCase(destination.getScheme()) || destination.getHost() == null) {
            throw new IllegalArgumentException("apiml.service.externalUrl must be an absolute HTTPS URL");
        }
        return destination.toASCIIString();
    }

    @Override
    public void destroy() {
        synchronized (TLSClientHelloExtractor.class) {
            if (redirectResponse != null && TLSClientHelloExtractor.USE_TLS_RESPONSE == redirectResponse) {
                TLSClientHelloExtractor.USE_TLS_RESPONSE = previousResponse;
            }
        }
    }
}
