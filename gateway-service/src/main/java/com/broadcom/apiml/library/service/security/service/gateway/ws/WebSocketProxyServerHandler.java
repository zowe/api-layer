/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.gateway.ws;

import com.broadcom.apiml.library.service.security.service.gateway.services.routing.RoutedService;
import com.broadcom.apiml.library.service.security.service.gateway.services.routing.RoutedServices;
import com.broadcom.apiml.library.service.security.service.gateway.services.routing.RoutedServicesUser;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import javax.inject.Singleton;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handle initialization and management of routed WebSocket sessions. Copies
 * data from the current session (from client to the gateway) to the server that
 * provides the real WebSocket service.
 */
@Component
@Singleton
public class WebSocketProxyServerHandler extends AbstractWebSocketHandler implements RoutedServicesUser {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(WebSocketProxyServerHandler.class);
    private final Map<String, WebSocketRoutedSession> routedSessions = new ConcurrentHashMap<>();
    private final Map<String, RoutedServices> routedServicesMap = new ConcurrentHashMap<>();
    private final DiscoveryClient discovery;
    private final SslContextFactory jettySslContextFactory;

    @Autowired
    public WebSocketProxyServerHandler(DiscoveryClient discovery, SslContextFactory jettySslContextFactory) {
        this.discovery = discovery;
        this.jettySslContextFactory = jettySslContextFactory;
        log.debug("Creating WebSocketProxyServerHandler {} jettySslContextFactory={}", this, jettySslContextFactory);
    }

    public void addRoutedServices(String serviceId, RoutedServices routedServices) {
        routedServicesMap.put(serviceId, routedServices);
    }

    public Map<String, WebSocketRoutedSession> getRoutedSessions() {
        return routedSessions;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        URI uri = webSocketSession.getUri();

        String[] uriParts = uri.getPath().split("/", 5);
        if (uriParts.length == 5) {
            String majorVersion = uriParts[2];
            String serviceId = uriParts[3];
            String path = uriParts[4];

            RoutedServices routedServices = routedServicesMap.get(serviceId);

            if (routedServices != null) {
                RoutedService service = routedServices.findServiceByGatewayUrl("ws/" + majorVersion);

                ServiceInstance serviceInstance = findServiceInstance(serviceId);
                if (serviceInstance != null) {
                    openWebSocketConnection(service, serviceInstance, serviceInstance, path, webSocketSession);
                } else {
                    webSocketSession.close(CloseStatus.SERVICE_RESTARTED.withReason(
                        String.format("Requested service %s does not have available instance", serviceId)));
                }
            } else {
                webSocketSession.close(CloseStatus.NOT_ACCEPTABLE.withReason(
                    String.format("Requested service %s is not known by the gateway", serviceId)));
            }
        } else {
            webSocketSession.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid URL format"));
        }
    }

    private void openWebSocketConnection(RoutedService service, ServiceInstance serviceInstance, Object uri,
                                         String path, WebSocketSession webSocketSession) throws IOException {
        String targetUrl = (serviceInstance.isSecure() ? "wss" : "ws") + "://" + serviceInstance.getHost() + ":"
            + serviceInstance.getPort() + service.getServiceUrl() + "/" + path;
        log.debug(String.format("Opening routed WebSocket session from %s to %s with %s by %s", uri.toString(), targetUrl, jettySslContextFactory, this));
        try {
            WebSocketRoutedSession session = new WebSocketRoutedSession(webSocketSession, targetUrl, jettySslContextFactory);
            routedSessions.put(webSocketSession.getId(), session);
        } catch (WebSocketProxyError e) {
            log.error("Error opening WebSocket connection to {}: {}", targetUrl, e.getMessage(), e);
            webSocketSession.close(CloseStatus.NOT_ACCEPTABLE.withReason(e.getMessage()));
        }
    }

    private ServiceInstance findServiceInstance(String serviceId) {
        List<ServiceInstance> serviceInstances = this.discovery.getInstances(serviceId);
        if (!serviceInstances.isEmpty()) {
            // TODO: This just a simple implementation that will be replaced by more sophisticated mechanism in future
            return serviceInstances.get(0);
        } else {
            return null;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.debug("afterConnectionClosed(session={},status={})", session, status);
        try {
            session.close(status);
            getRoutedSession(session).close(status);
            routedSessions.remove(session.getId());
        } catch (NullPointerException | IOException e) {
            log.debug("Error closing WebSocket connection: {}", e.getMessage(), e);
        }
    }

    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage)
        throws Exception {
        log.debug("handleMessage(session={},message={})", webSocketSession, webSocketMessage);
        WebSocketRoutedSession session = getRoutedSession(webSocketSession);
        if (session != null) {
            session.sendMessageToServer(webSocketMessage);
        }
    }

    private WebSocketRoutedSession getRoutedSession(WebSocketSession webSocketSession) {
        return routedSessions.get(webSocketSession.getId());
    }
}
