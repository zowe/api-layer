/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ws;

import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContexts;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketProxyServerHandlerTest {
    private WebSocketSession session;
    private WebSocketProxyServerHandler webSocketProxyServerHandler;
    private SslContextFactory jettySslContextFactory;
    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);


    @BeforeEach
    public void setup() {
        session = mock(WebSocketSession.class);
//        session = new StandardWebSocketSession();
//        jettySslContextFactory = mock(SslContextFactory.class);
        jettySslContextFactory = new SslContextFactory();
        webSocketProxyServerHandler = new WebSocketProxyServerHandler(discoveryClient, jettySslContextFactory);
    }

//    @Test
    public void should() throws Exception {
        jettySslContextFactory.setKeyStoreType("PKCS12");
        jettySslContextFactory.setCertAlias("localhost");
        jettySslContextFactory.setTrustStoreType("type");
        jettySslContextFactory.setProtocol("SSL");
        jettySslContextFactory.setKeyStorePath("src/test/resources/certs/keystore.jks");
        jettySslContextFactory.setTrustStorePath("src/test/resources/certs/truststore.jks");
        jettySslContextFactory.setKeyManagerFactoryAlgorithm("SunX509");
        jettySslContextFactory.setTrustManagerFactoryAlgorithm("PKIX");
        jettySslContextFactory.setKeyStorePassword("pass");
        jettySslContextFactory.setTrustStorePassword("pass");
        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            public boolean isTrusted(X509Certificate[] certificate, String authType) {
                return true;
            }
        };
        SSLContext sslContext = SSLContexts.custom()
            .loadTrustMaterial(null, acceptingTrustStrategy).build();
//        SSLContext sslContext = SSLContext.getInstance("SSL");

        jettySslContextFactory.setSslContext(sslContext);
//        when(session.isOpen()).thenReturn(true);
        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(
            new RoutedService("api-v1", "api/v1", "/service/api/v1"));
        routedServices.addRoutedService(
            new RoutedService("ui-v1", "ui/v1", "/service"));
        routedServices.addRoutedService(
            new RoutedService("ws-v1", "ws/v1", "/service/ws"));
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        headers.add("X-Test", "value");
        when(session.getUri()).thenReturn(new URI("/ws/v1/service/uppercase"));
        when(discoveryClient.getInstances("service")).thenReturn(
            Collections.singletonList(new DefaultServiceInstance("service", "localhost", 80, false, null)));
        when(session.getHandshakeHeaders()).thenReturn(headers);
        webSocketProxyServerHandler.addRoutedServices("service", routedServices);
        webSocketProxyServerHandler.afterConnectionEstablished(session);
        assertTrue(session.isOpen());

    }


}
