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

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.commons.attls.ContextIsNotInitializedException;
import org.zowe.commons.attls.InboundAttls;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

class ApimlTomcatCustomizerTest {

    @Test
    void providedCorrectProtocolInConnector_endpointIsConfigured() {
        ApimlTomcatCustomizer<?, ?> customizer = new ApimlTomcatCustomizer<>();
        customizer.afterPropertiesSet();
        Http11NioProtocol protocol = new Http11NioProtocol();
        Connector connector = new Connector(protocol);
        customizer.customizeConnector(connector);
        Http11NioProtocol protocolHandler = (Http11NioProtocol) connector.getProtocolHandler();
        AbstractEndpoint<?, ?> abstractEndpoint = ReflectionTestUtils.invokeMethod(protocolHandler, "getEndpoint");
        assumeTrue(abstractEndpoint != null);
        assertEquals(ApimlTomcatCustomizer.ApimlAttlsHandler.class, abstractEndpoint.getHandler().getClass());
    }

    @Test
    @Disabled //TODO: finish the adaptation of the test to the new SocketChannelImpl class
    @SuppressWarnings({"rawtypes", "unchecked"})
    void whenSocketArrives_fileDescriptorIsObtained() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ContextIsNotInitializedException {
        AbstractEndpoint.Handler handler = mock(AbstractEndpoint.Handler.class);
        NioChannel socket = mock(NioChannel.class);

        Constructor<SocketChannel> channelConstructor = (Constructor<SocketChannel>) Class.forName("sun.nio.ch.SocketChannelImpl").getDeclaredConstructor(SelectorProvider.class, ProtocolFamily.class, FileDescriptor.class, SocketAddress.class);
        channelConstructor.setAccessible(true);
        FileDescriptor fd = new FileDescriptor();
        ReflectionTestUtils.setField(fd, "fd", 608);
        SocketChannel socketChannel = channelConstructor.newInstance(null, StandardProtocolFamily.INET, fd, null);
        ApimlTomcatCustomizer.ApimlAttlsHandler apimlAttlsHandler = new ApimlTomcatCustomizer.ApimlAttlsHandler(handler);

        when(socket.getIOChannel()).thenReturn(socketChannel);
        SocketWrapperBase socketWrapper = getSocketWarapper(socket);
        doAnswer(answer -> {
            int fdNumber = (int) ReflectionTestUtils.getField(InboundAttls.get(), "id");
            assertEquals(608, fdNumber);
            return AbstractEndpoint.Handler.SocketState.OPEN;
        }).when(handler).process(socketWrapper, SocketEvent.OPEN_READ);
        assertEquals(AbstractEndpoint.Handler.SocketState.OPEN, apimlAttlsHandler.process(socketWrapper, SocketEvent.OPEN_READ));


    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    SocketWrapperBase getSocketWarapper(NioChannel socket) {
        return new SocketWrapperBase(socket, new NioEndpoint()) {

            @Override
            protected boolean flushNonBlocking() throws IOException {
                return true;
            }

            @Override
            protected void populateRemoteHost() {

            }

            @Override
            protected void populateRemoteAddr() {

            }

            @Override
            protected void populateRemotePort() {

            }

            @Override
            protected void populateLocalName() {

            }

            @Override
            protected void populateLocalAddr() {

            }

            @Override
            protected void populateLocalPort() {

            }

            @Override
            public int read(boolean block, byte[] b, int off, int len) throws IOException {
                return 0;
            }

            @Override
            public int read(boolean block, ByteBuffer to) throws IOException {
                return 0;
            }

            @Override
            public boolean isReadyForRead() throws IOException {
                return false;
            }

            @Override
            public void setAppReadBufHandler(ApplicationBufferHandler handler) {

            }

            @Override
            protected void doClose() {

            }

            @Override
            protected void doWrite(boolean block, ByteBuffer from) throws IOException {

            }

            @Override
            public void registerReadInterest() {

            }

            @Override
            public void registerWriteInterest() {

            }

            @Override
            public SendfileDataBase createSendfileData(String filename, long pos, long length) {
                return null;
            }

            @Override
            public SendfileState processSendfile(SendfileDataBase sendfileData) {
                return null;
            }

            @Override
            public void doClientAuth(SSLSupport sslSupport) throws IOException {

            }

            @Override
            public SSLSupport getSslSupport(String clientCertProvider) {
                return null;
            }

            @Override
            protected OperationState newOperationState(boolean read, ByteBuffer[] buffers, int offset, int length, BlockingMode block, long timeout, TimeUnit unit, Object attachment, CompletionCheck check, CompletionHandler handler, Semaphore semaphore, VectoredIOCompletionHandler completion) {
                return null;
            }

            @Override
            public SSLSupport getSslSupport() {
                return null;
            }
        };
    }
}
