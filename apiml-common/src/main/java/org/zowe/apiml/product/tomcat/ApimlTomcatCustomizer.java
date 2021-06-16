/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.tomcat;

import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.*;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;
import org.zowe.commons.attls.InboundAttls;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.SocketChannel;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Set;

@Component
public class ApimlTomcatCustomizer<S, U> implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {


    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers(connector -> {
            Http11NioProtocol protocolHandler = (Http11NioProtocol) connector.getProtocolHandler();
            try {

                Field handlerField = AbstractProtocol.class.getDeclaredField("handler");
                handlerField.setAccessible(true);

                AbstractEndpoint.Handler<S> handler = (AbstractEndpoint.Handler<S>) handlerField.get(protocolHandler);
                handler = new ApimlAttlHandler<S>(handler);
                Method method = AbstractProtocol.class.getDeclaredMethod("getEndpoint");
                method.setAccessible(true);
                AbstractEndpoint<S, U> abstractEndpoint = (AbstractEndpoint<S, U>) method.invoke(protocolHandler);

                abstractEndpoint.setHandler(handler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static class ApimlAttlHandler<S> implements AbstractEndpoint.Handler<S> {

        private final AbstractEndpoint.Handler<S> handler;

        public ApimlAttlHandler(AbstractEndpoint.Handler<S> handler) {
            this.handler = handler;
        }

        @Override
        public SocketState process(SocketWrapperBase<S> socket, SocketEvent status) {
            NioChannel secureChannel = (NioChannel) socket.getSocket();
            SocketChannel socketChannel = secureChannel.getIOChannel();
            try {
                Class<?> socketChannelImpl = Class.forName("sun.nio.ch.SocketChannelImpl");
                Method getFDVal = socketChannelImpl.getDeclaredMethod("getFDVal");
                getFDVal.setAccessible(true);
                int fileDescriptor = (int) getFDVal.invoke(socketChannel);
                System.out.println("method: " + fileDescriptor);
                InboundAttls.init(fileDescriptor);
                if ("z/os".equalsIgnoreCase(System.getProperty("os.name"))) {
                    if(InboundAttls.getCertificate() != null) {
                        try {
                            InputStream targetStream = new ByteArrayInputStream(InboundAttls.getCertificate());

                            X509Certificate certificate = (X509Certificate) CertificateFactory
                                .getInstance("X509")
                                .generateCertificate(targetStream);
                            System.out.println("user id:" + certificate.toString());
                        } catch (Exception e){
                            System.err.println("Error reading cert: " + e);
                        }
                    } else {
                        System.out.println("no cert in attls context");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return handler.process(socket, status);
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
