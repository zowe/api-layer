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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.tomcat.reactive.TomcatReactiveWebServerFactory;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ResourceLock("tomcat-use-tls-response")
class HttpsRedirectConfigurationTest {

    @Test
    void disabledByDefault() {
        byte[] original = TLSClientHelloExtractor.USE_TLS_RESPONSE;
        new ApplicationContextRunner().withUserConfiguration(HttpsRedirectConfiguration.class).run(context -> {
            assertTrue(context.getBeansOfType(HttpsRedirectConfiguration.class).isEmpty());
            assertSame(original, TLSClientHelloExtractor.USE_TLS_RESPONSE);
        });
    }

    @Test
    void installsFixedResponseWithoutChangingProtocolAndRestoresItOnShutdown() {
        byte[] original = TLSClientHelloExtractor.USE_TLS_RESPONSE;
        new ApplicationContextRunner().withUserConfiguration(HttpsRedirectConfiguration.class)
            .withPropertyValues("apiml.security.httpRedirect.enabled=true",
                "apiml.service.externalUrl=https://public.example:7554/")
            .run(context -> {
                var factory = new TomcatReactiveWebServerFactory();
                String protocol = factory.getProtocol();
                var configuration = context.getBean(HttpsRedirectConfiguration.class);
                configuration.customize(factory);
                configuration.customize(factory);
                assertEquals(protocol, factory.getProtocol());
                assertEquals("HTTP/1.1 308 Permanent Redirect\r\nLocation: https://public.example:7554/\r\n"
                    + "Content-Length: 0\r\nConnection: close\r\n\r\n",
                    new String(TLSClientHelloExtractor.USE_TLS_RESPONSE, StandardCharsets.US_ASCII));
            });
        assertSame(original, TLSClientHelloExtractor.USE_TLS_RESPONSE);
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,true"})
    void leavesPlainHttpAndAttlsUnchanged(boolean sslEnabled, boolean attlsEnabled) {
        byte[] original = TLSClientHelloExtractor.USE_TLS_RESPONSE;
        var configuration = new HttpsRedirectConfiguration(sslEnabled, attlsEnabled, "");
        configuration.customize(new TomcatReactiveWebServerFactory());
        configuration.destroy();
        assertSame(original, TLSClientHelloExtractor.USE_TLS_RESPONSE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "http://example.com", "https://user:password@example.com", "https://example.com/path",
        "https://example.com?q=1", "https://example.com/#fragment", "https://example.com:99999",
        "https://example.com:0", "https://example.com\r\nLocation: evil"
    })
    void rejectsInvalidDestinationsBeforeChangingTheResponse(String destination) {
        byte[] original = TLSClientHelloExtractor.USE_TLS_RESPONSE;
        var configuration = new HttpsRedirectConfiguration(true, false, destination);
        var exception = assertThrows(IllegalArgumentException.class,
            () -> configuration.customize(new TomcatReactiveWebServerFactory()));
        assertTrue(exception.getMessage().contains("apiml.service.externalUrl"));
        assertSame(original, TLSClientHelloExtractor.USE_TLS_RESPONSE);
    }

    @Test
    void supportsIpv6Destinations() {
        var configuration = new HttpsRedirectConfiguration(true, false, "https://[::1]:7554");
        try {
            configuration.customize(new TomcatReactiveWebServerFactory());
            assertTrue(new String(TLSClientHelloExtractor.USE_TLS_RESPONSE, StandardCharsets.US_ASCII)
                .contains("Location: https://[::1]:7554/\r\n"));
        } finally {
            configuration.destroy();
        }
    }
}
