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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.RoutedServicesUser;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handle initialization and management of routed WebSocket sessions. Copies
 * data from the current session (from client to the gateway) to the server that
 * provides the real WebSocket service.
 */
@Component
@Singleton
@Slf4j
public class WebSocketProxyServerHandler extends AbstractWebSocketHandler implements RoutedServicesUser, SubProtocolCapable, ClientSessionFailureCallback, ClientSessionSuccessCallback {

    @Value("${server.webSocket.supportedProtocols:-}")
    private List<String> subProtocols;

    @Override
    public List<String> getSubProtocols() {
        return subProtocols;
    }

    private final Map<String, WebSocketRoutedSession> routedSessions;
    private final Map<String, RoutedServices> routedServicesMap = new ConcurrentHashMap<>();
    private final WebSocketRoutedSessionFactory webSocketRoutedSessionFactory;
    private final WebSocketClientFactory webSocketClientFactory;
    private static final String SEPARATOR = "/";
    private final LoadBalancerClient lbCLient;
    private ApplicationContext context;
    private WebSocketProxyServerHandler meAsProxy;

    @Autowired
    public WebSocketProxyServerHandler(WebSocketClientFactory webSocketClientFactory, LoadBalancerClient lbCLient, ApplicationContext context) {
        this.webSocketClientFactory = webSocketClientFactory;
        this.routedSessions = new ConcurrentHashMap<>();  // Default
        this.webSocketRoutedSessionFactory = new WebSocketRoutedSessionFactoryImpl();
        this.lbCLient = lbCLient;
        this.context = context;
        log.debug("Creating WebSocketProxyServerHandler {} ", this);
    }

    @PostConstruct
    private void initBean() {
        meAsProxy = context.getBean(WebSocketProxyServerHandler.class);
    }

    public WebSocketProxyServerHandler(WebSocketClientFactory webSocketClientFactory,
                                       Map<String, WebSocketRoutedSession> routedSessions, WebSocketRoutedSessionFactory webSocketRoutedSessionFactory, LoadBalancerClient lbCLient) {
        this.webSocketClientFactory = webSocketClientFactory;
        this.routedSessions = routedSessions;
        this.webSocketRoutedSessionFactory = webSocketRoutedSessionFactory;
        this.lbCLient = lbCLient;
        log.debug("Creating WebSocketProxyServerHandler {}", this);
    }

    public void addRoutedServices(String serviceId, RoutedServices routedServices) {
        routedServicesMap.put(serviceId, routedServices);
    }

    private String getTargetUrl(String serviceUrl, ServiceInstance serviceInstance, String path) {
        String servicePath = serviceUrl.charAt(serviceUrl.length() - 1) == '/' ? serviceUrl : serviceUrl + SEPARATOR;
        return (serviceInstance.isSecure() ? "wss" : "ws") + "://" + serviceInstance.getHost() + ":"
            + serviceInstance.getPort() + servicePath + path;
    }

    public Map<String, WebSocketRoutedSession> getRoutedSessions() {
        return routedSessions;
    }

    // This is the client (usually a browser) to API ML Gateway session
    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws IOException {
        String[] uriParts = getUriParts(webSocketSession);
        if (uriParts == null || uriParts.length != 5) {
            closeWebSocket(webSocketSession, CloseStatus.NOT_ACCEPTABLE, "Invalid URL format");
            return;
        }

        String majorVersion;
        String serviceId;
        String path = uriParts[4];

        majorVersion = uriParts[3];
        serviceId = uriParts[1];

        routeToService(webSocketSession, serviceId, majorVersion, path);
    }

    private void routeToService(WebSocketSession webSocketSession, String serviceId, String majorVersion, String path) throws IOException {
        RoutedServices routedServices = routedServicesMap.get(serviceId);

        if (routedServices == null) {
            closeWebSocket(webSocketSession, CloseStatus.NOT_ACCEPTABLE,
                String.format("Requested service %s is not known by the gateway", serviceId));
            return;
        }

        RoutedService service = routedServices.findServiceByGatewayUrl("ws/" + majorVersion);
        if (service == null) {
            closeWebSocket(webSocketSession, CloseStatus.NOT_ACCEPTABLE,
                String.format("Requested ws/%s url is not known by the gateway", majorVersion));
            return;
        }

        try {
            log.debug("Trying to open a WebSocket connection and route to the {} service", serviceId);
            meAsProxy.openConn(serviceId, service, webSocketSession, path);
        } catch (WebSocketProxyError e) {
            log.debug("Error opening WebSocket connection to: {}, {}", service.getServiceUrl(), e.getMessage());
            webSocketSession.close(CloseStatus.NOT_ACCEPTABLE.withReason(e.getMessage()));
        }
    }

    @Retryable(value = WebSocketProxyError.class, backoff = @Backoff(value = 1000))
    void openConn(String serviceId, RoutedService service, WebSocketSession webSocketSession, String path) throws IOException {
        ServiceInstance serviceInstance = this.lbCLient.choose(serviceId);
        if (serviceInstance != null) {
            openWebSocketConnection(service, serviceInstance, serviceInstance, path, webSocketSession);
        } else {
            closeWebSocket(webSocketSession, CloseStatus.SERVICE_RESTARTED,
                String.format("Requested service %s does not have available instance", serviceId));
        }
    }

    private void closeWebSocket(WebSocketSession webSocketSession, CloseStatus closeStatus, String reason) throws IOException {
        if (webSocketSession.isOpen()) {
            log.debug("WebSocket session {} is open, requesting close with reason {}", webSocketSession.getId(), reason);
            webSocketSession.close(closeStatus.withReason(reason));
        } else {
            log.debug("WebSocket session {} is already closed, new reason is {}", webSocketSession.getId(), reason);
        }
    }

    private String[] getUriParts(WebSocketSession webSocketSession) {
        URI uri = webSocketSession.getUri();
        String[] uriParts = null;
        if (uri != null && uri.getPath() != null) {
            uriParts = uri.getPath().split(SEPARATOR, 5);
        }
        return uriParts;
    }

    private void openWebSocketConnection(RoutedService service, ServiceInstance serviceInstance, Object uri,
                                         String path, WebSocketSession webSocketSession) {
        String serviceUrl = service.getServiceUrl();
        String targetUrl = getTargetUrl(serviceUrl, serviceInstance, path);

        log.debug(String.format("Opening routed WebSocket session from %s to %s with %s by %s", uri.toString(), targetUrl, webSocketClientFactory, this));

        webSocketRoutedSessionFactory.session(webSocketSession, targetUrl, webSocketClientFactory, this, this);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // if the browser closes the session, close the GWs client one as well.
        Optional.ofNullable(routedSessions.get(session.getId()))
            .filter(routedSession -> routedSession.isClientConnected())
            .map(WebSocketRoutedSession::getClientSession)
            .ifPresent(clientSession -> {
                try {
                    clientSession.close(status);
                } catch (IOException e) {
                    log.debug("Error closing WebSocket client connection: {}", e.getMessage());
                }
            });
        routedSessions.remove(session.getId());
    }

    private void close(WebSocketRoutedSession webSocketRoutedSession, CloseStatus status) {
        log.debug("close(webSocketRoutedSession={},status={})", webSocketRoutedSession, status);
        if (webSocketRoutedSession == null) return;

        try {
            webSocketRoutedSession.close(status);
        } catch (IOException e) {
            log.debug("Error closing WebSocket connection: {}", e.getMessage(), e);
        }
    }

    private void close(WebSocketSession session, CloseStatus status) {
        log.debug("close(session={},status={})", session, status);
        try {
            session.close(status);
        } catch (IOException e) {
            log.debug("Error closing WebSocket connection: {}", e.getMessage(), e);
        } finally {
            routedSessions.remove(session.getId());
            close(getRoutedSession(session), status);
        }
    }

    @Recover
    void closeServerSession(ServerNotYetAvailableException e, WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage) {
        log.debug("Trying to close Server WebSocket session {} because of too many ServerNotYetAvailableException failures", webSocketSession.getId());
        try {
            webSocketSession.close(CloseStatus.SESSION_NOT_RELIABLE);
        } catch (IOException e1) {
            log.debug("Failed closing Server WebSocket session {}", webSocketSession.getId());
        }
    }

    @Override
    @Retryable(
        include = ServerNotYetAvailableException.class,
        backoff = @Backoff(value = 6000),
        recover = "closeServerSession",
        maxAttempts = 5
    )
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage) {
        log.debug("handleMessage(session={}, message={})", webSocketSession, webSocketMessage);
        WebSocketRoutedSession session = getRoutedSession(webSocketSession);

        if (session == null) {
            throw new ServerNotYetAvailableException();
        }

        try {
            if (!isClientConnectionReady(session)) {
                log.debug("Client Session for Server session {} is available but not ready", webSocketSession.getId());
                throw new ServerNotYetAvailableException();
            } else if (isClientConnectionClosed(session)) {
                closeServerSession(null, webSocketSession, webSocketMessage);
            }
            session.sendMessageToServer(webSocketMessage);
        } catch (Exception ex) {
            log.debug("Error sending WebSocket message. Closing session due to exception:", ex);
            close(webSocketSession, CloseStatus.SESSION_NOT_RELIABLE);
        }
    }

    private boolean isClientConnectionClosed(WebSocketRoutedSession session) {
        WebSocketSession clientSession = session.getClientSession();
        return session != null && clientSession != null && !clientSession.isOpen();
    }

    private boolean isClientConnectionReady(WebSocketRoutedSession session) {
        return session != null && session.isClientConnected();
    }

    private WebSocketRoutedSession getRoutedSession(WebSocketSession webSocketSession) {
        return routedSessions.get(webSocketSession.getId());
    }

    @Override
    public void onClientSessionSuccess(WebSocketRoutedSession routedSession, WebSocketSession serverSession, WebSocketSession clientSession) {
        routedSessions.put(serverSession.getId(), routedSession);
    }

    @Override
    public void onClientSessionFailure(WebSocketRoutedSession routedSession, WebSocketSession serverSession, Throwable throwable) {
        log.debug("Failed opening client web socket session against {}. Server WebSocket session is {}", routedSession.getTargetUrl(), serverSession.getId(), throwable);
        try {
            routedSession.getClientHandler().handleMessage(serverSession, new TextMessage("Failed opening a session against " + routedSession.getTargetUrl() + ": " + throwable.getMessage()));
            serverSession.close(CloseStatus.SERVER_ERROR);
        } catch (Exception e) {
            log.debug("Failed sending / closing WebSocket session", e);
        }
    }
}
