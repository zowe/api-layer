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
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.tomcat.reactive.TomcatReactiveWebServerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpsRedirectConfigurationTest {

    @Test
    void enabledConfigurationSelectsRedirectProtocol() {
        new ApplicationContextRunner().withUserConfiguration(HttpsRedirectConfiguration.class)
            .withPropertyValues("apiml.security.httpRedirect.enabled=true")
            .run(context -> {
                assertNull(context.getStartupFailure());
                var factory = new TomcatReactiveWebServerFactory();
                context.getBean(HttpsRedirectConfiguration.class).customize(factory);
                assertEquals(HttpsRedirectProtocol.class.getName(), factory.getProtocol());
            });
    }

    @Test
    void disabledByDefault() {
        new ApplicationContextRunner().withUserConfiguration(HttpsRedirectConfiguration.class)
            .run(context -> assertTrue(context.getBeansOfType(HttpsRedirectConfiguration.class).isEmpty()));
    }

    @Test
    void preservesAttlsAndPlainHttpProtocols() {
        for (var configuration : new HttpsRedirectConfiguration[]{
            new HttpsRedirectConfiguration(false, false, "", "localhost"),
            new HttpsRedirectConfiguration(false, true, "", "localhost"),
            new HttpsRedirectConfiguration(true, true, "", "localhost")
        }) {
            var factory = new TomcatReactiveWebServerFactory();
            String original = factory.getProtocol();
            configuration.customize(factory);
            assertEquals(original, factory.getProtocol());
            assertTrue(factory.getConnectorCustomizers().isEmpty());
        }
    }

    @Test
    void usesFinalConnectorPortAndConfiguredPublicOrigin() {
        var factory = new TomcatReactiveWebServerFactory();
        factory.setPort(10010);
        new HttpsRedirectConfiguration(true, false, "https://public.example:443", "internal.example")
            .customize(factory);
        var main = spy(new HttpsRedirectProtocol());
        var extra = spy(new HttpsRedirectProtocol());
        var mainConnector = new Connector(main);
        var extraConnector = new Connector(extra);
        for (var customizer : factory.getConnectorCustomizers()) {
            customizer.customize(mainConnector);
            customizer.customize(extraConnector);
        }
        mainConnector.setPort(10010);
        extraConnector.setPort(10011);
        for (Connector connector : new Connector[]{mainConnector, extraConnector}) {
            for (var listener : connector.findLifecycleListeners()) {
                listener.lifecycleEvent(new LifecycleEvent(connector, Lifecycle.BEFORE_INIT_EVENT, null));
            }
        }
        verify(main).setHttpsRedirectOrigin("https://public.example:443");
        verify(extra).setHttpsRedirectOrigin("https://internal.example:10011");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://example.com", "https://user:password@example.com", "https://example.com/path",
        "https://example.com?q=1", "https://example.com/#fragment", "https://example.com:99999",
        "https://example.com:0", "https://example.com\r\nLocation: evil"
    })
    void rejectsInvalidRedirectOrigins(String origin) {
        assertThrows(IllegalArgumentException.class, () -> new HttpsRedirectProtocol().setHttpsRedirectOrigin(origin));
    }
}
