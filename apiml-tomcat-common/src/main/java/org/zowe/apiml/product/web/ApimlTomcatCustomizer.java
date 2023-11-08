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

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.SocketEvent;
import org.apache.tomcat.util.net.SocketWrapperBase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;
import org.zowe.apiml.exception.AttlsHandlerException;
import org.zowe.commons.attls.InboundAttls;

import javax.annotation.PostConstruct;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.SocketChannel;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "server.attls.enabled", havingValue = "true")
@Slf4j
public class ApimlTomcatCustomizer<S, U> implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @PostConstruct
    public void afterPropertiesSet() {
        log.debug("AT-TLS mode is enabled");
    }

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        InboundAttls.setAlwaysLoadCertificate(true);
        factory.addConnectorCustomizers(this::customizeConnector);
    }

    public void customizeConnector(Connector connector) {
        Http11NioProtocol protocolHandler = (Http11NioProtocol) connector.getProtocolHandler();
        try {

            Field handlerField = AbstractProtocol.class.getDeclaredField("handler");
            handlerField.setAccessible(true);
            AbstractEndpoint.Handler<S> handler = (AbstractEndpoint.Handler<S>) handlerField.get(protocolHandler);
            handler = new ApimlAttlsHandler<>(handler);
            Method method = AbstractProtocol.class.getDeclaredMethod("getEndpoint");
            method.setAccessible(true);
            AbstractEndpoint<S, U> abstractEndpoint = (AbstractEndpoint<S, U>) method.invoke(protocolHandler);
            abstractEndpoint.setHandler(handler);
        } catch (Exception e) {
            throw new AttlsHandlerException("Not able to add handler.", e);
        }
    }

    public static class ApimlAttlsHandler<S> implements AbstractEndpoint.Handler<S> {

        private final AbstractEndpoint.Handler<S> handler;

        public ApimlAttlsHandler(AbstractEndpoint.Handler<S> handler) {
            this.handler = handler;
        }

        @Override
        public SocketState process(SocketWrapperBase<S> socket, SocketEvent status) {
            NioChannel secureChannel = (NioChannel) socket.getSocket();
            SocketChannel socketChannel = secureChannel.getIOChannel();
            try {
                Class<?> socketChannelImpl = Class.forName("sun.nio.ch.SocketChannelImpl");
                Field fdField = socketChannelImpl.getDeclaredField("fdVal");
                fdField.setAccessible(true);
                int fileDescriptor = fdField.getInt(socketChannel);
                InboundAttls.init(fileDescriptor);
                return handler.process(socket, status);
            } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                throw new AttlsHandlerException("Different implementation expected.", e);
            } finally {
                InboundAttls.dispose();
            }

        }

        @Override
        public Object getGlobal() {

            return handler.getGlobal();
        }

        @Override
        public Set<S> getOpenSockets() {
            return handler.getOpenSockets();
        }

        @Override
        public void release(SocketWrapperBase<S> socketWrapper) {
            handler.release(socketWrapper);
        }

        @Override
        public void pause() {
            handler.pause();
        }

        @Override
        public void recycle() {
            handler.recycle();
        }
    }
}
