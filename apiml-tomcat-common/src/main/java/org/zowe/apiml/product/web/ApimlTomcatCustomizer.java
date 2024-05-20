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

import jakarta.annotation.PostConstruct;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.stereotype.Component;
import org.zowe.apiml.exception.AttlsHandlerException;
import org.zowe.commons.attls.ContextIsNotInitializedException;
import org.zowe.commons.attls.InboundAttls;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SocketChannel;

@Slf4j
@Component
@ConditionalOnProperty(name = "server.attls.enabled", havingValue = "true")
public class ApimlTomcatCustomizer implements TomcatConnectorCustomizer {

    private static final String INCOMPATIBLE_VERSION_MESSAGE = "AT-TLS-Incompatible configuration. Verify AT-TLS requirements: Java version, Tomcat version. Exception message: ";

    @PostConstruct
    public void afterPropertiesSet() {
        log.debug("AT-TLS mode is enabled");
        InboundAttls.setAlwaysLoadCertificate(true);
    }

    @Override
    public void customize(Connector connector) {
        Http11NioProtocol protocolHandler = (Http11NioProtocol) connector.getProtocolHandler();
        try {
            Field handlerField = AbstractProtocol.class.getDeclaredField("handler");
            handlerField.setAccessible(true);
            AbstractEndpoint.Handler<Object> handler = (AbstractEndpoint.Handler<Object>) handlerField.get(protocolHandler);
            handler = new ApimlAttlsHandler<>(handler);
            Method method = AbstractProtocol.class.getDeclaredMethod("getEndpoint");
            method.setAccessible(true);
            AbstractEndpoint<Object, Object> abstractEndpoint = (AbstractEndpoint<Object, Object>) method.invoke(protocolHandler);
            abstractEndpoint.setHandler(handler);
        } catch (Exception e) {
            throw new AttlsHandlerException("Not able to add handler.", e);
        }
    }

    public static class ApimlAttlsHandler<S> implements AbstractEndpoint.Handler<S> {

        @Delegate(excludes = Overridden.class)
        private final AbstractEndpoint.Handler<S> handler;

        // this field cannot be final for testing purpose, but using is the same as final
        @SuppressWarnings("squid:S3008")
        private static  /*final*/ Field ASYNCHRONOUS_SOCKET_CHANNEL_FD ;
        private static   Field FILE_DESCRIPTOR_FD;

        private static  Method SOCKET_CHANNEL_GET_FDVAL_METHOD;

        public ApimlAttlsHandler(AbstractEndpoint.Handler<S> handler) {
            this.handler = handler;
            try {
                Class<?> nioClazz = Class.forName("sun.nio.ch.SocketChannelImpl");

                SOCKET_CHANNEL_GET_FDVAL_METHOD = nioClazz.getMethod("getFDVal");
                SOCKET_CHANNEL_GET_FDVAL_METHOD.setAccessible(true);

                // obtain field to get file descriptor if NIO2 is using
                Class<?> nio2Clazz = Class.forName("sun.nio.ch.AsynchronousSocketChannelImpl");
                ASYNCHRONOUS_SOCKET_CHANNEL_FD = nio2Clazz.getDeclaredField("fd");
                ASYNCHRONOUS_SOCKET_CHANNEL_FD.setAccessible(true);

                // obtain fd field in FileDescriptor class
                FILE_DESCRIPTOR_FD = FileDescriptor.class.getDeclaredField("fd");
                FILE_DESCRIPTOR_FD.setAccessible(true);
            } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | SecurityException e) {
                throw new IllegalStateException(INCOMPATIBLE_VERSION_MESSAGE + e.getMessage(), e);
            }
        }

        /**
         * This method handle processing of each request. At first create AT-TLS context, the process and on the end
         * dispose the context.
         * @param socketWrapperBase describing socket (see Tomcat implementation)
         * @param status status of socket (see Tomcat implementation)
         * @return new status of socket (see Tomcat implementation)
         */
        public SocketState process(SocketWrapperBase socketWrapperBase, SocketEvent status) {
            final int fdVal = getFd(socketWrapperBase.getSocket());

            InboundAttls.init(fdVal);
            try {
                return handler.process(socketWrapperBase, status);
            } finally {
                try {
                    InboundAttls.clean();
                } catch (ContextIsNotInitializedException e) {
                    log.debug("Cannot clean AT-TLS context");
                }
                InboundAttls.dispose();
            }
        }

        private int getFd(Object socket) {
            try {
                if (socket instanceof NioChannel nioChannel) {
                    return getFd(nioChannel);
                } else if (socket instanceof Nio2Channel nio2Channel) {
                    return getFdAsync(nio2Channel);
                } else if (socket instanceof Long socketNioChannel) { // APR uses Long as socket to identify
                    return socketNioChannel.intValue();
                } else {
                    throw new IllegalStateException("Socket " + socket.getClass() + " is not supported for AT-TLS");
                }
            } catch (IllegalArgumentException | IllegalAccessException | IllegalStateException |
                     InvocationTargetException e) {
                throw new IllegalStateException(INCOMPATIBLE_VERSION_MESSAGE + e.getMessage(), e);
            }
        }

        int getFd(NioChannel socket) throws InvocationTargetException, IllegalAccessException {
            SocketChannel socketChannel = socket.getIOChannel();
            if (socketChannel == null) {
                throw new IllegalStateException("Socket channel is not initialized");
            }
            return (int) SOCKET_CHANNEL_GET_FDVAL_METHOD.invoke(socketChannel);
        }

        int getFdAsync(Nio2Channel socket) throws IllegalAccessException {
            AsynchronousSocketChannel asch = socket.getIOChannel();
            if (asch == null) {
                throw new IllegalStateException("Asynchronous socket channel is not initialized");
            }
            FileDescriptor fd = (FileDescriptor) ASYNCHRONOUS_SOCKET_CHANNEL_FD.get(asch);
            if (fd == null) {
                throw new IllegalStateException("File descriptor is not set in the asynchronous socket channel");
            }
            return FILE_DESCRIPTOR_FD.getInt(fd);
        }

        interface Overridden {
            <S> SocketState process(SocketWrapperBase<S> socket, SocketEvent status);
        }

    }

}
