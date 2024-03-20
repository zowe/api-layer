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
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.ajp.AjpNio2Protocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.NioEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TomcatAcceptFixConfigTest {

    private TomcatAcceptFixConfig tomcatAcceptFixConfig;
    private SocketChannel socketChannel;
    private TestServerSocketChannel serverSocket;
    private TestEndpoint testEndpoint;
    private Connector connector;

    private void fireEventStarted(Connector connector, LifecycleState lifecycleState) {
        Lifecycle lifecycle = mock(Lifecycle.class);
        doReturn(lifecycleState).when(lifecycle).getState();
        LifecycleEvent lifecycleEvent = new LifecycleEvent(lifecycle, "type", "data");
        Stream.of(connector.findLifecycleListeners()).forEach(l -> l.lifecycleEvent(lifecycleEvent));
    }

    private void customizeConnector() {
        tomcatAcceptFixConfig = new TomcatAcceptFixConfig();
        tomcatAcceptFixConfig.retryRebindTimeoutSecs = 0;
        TomcatConnectorCustomizer tomcatConnectorCustomizer = tomcatAcceptFixConfig.tomcatAcceptorFix();
        tomcatConnectorCustomizer.customize(connector);
        fireEventStarted(connector, LifecycleState.STARTED);
    }

    private Connector createCustomizer(ProtocolHandler protocolHandler) {
        TomcatAcceptFixConfig tomcatAcceptFixConfig = new TomcatAcceptFixConfig();
        TomcatConnectorCustomizer tomcatConnectorCustomizer = tomcatAcceptFixConfig.tomcatAcceptorFix();
        Connector connector = new Connector(protocolHandler);
        tomcatConnectorCustomizer.customize(connector);
        return connector;
    }

    @BeforeEach
    void setUp() {
        SelectorProvider provider = mock(SelectorProvider.class);
        socketChannel = mock(SocketChannel.class);
        serverSocket = spy(new TestServerSocketChannel(provider));
        testEndpoint = spy(new TestEndpoint(serverSocket));
        TestProtocol testProtocol = spy(new TestProtocol(testEndpoint));
        connector = new Connector(testProtocol);

        customizeConnector();
    }

    @Test
    void givenCustomizedConnector_whenTcpipIsReady_thenReturnAccepted() throws Exception {
        doReturn(socketChannel).when(serverSocket).accept();
        assertSame(socketChannel, testEndpoint.serverSocketAccept());
    }

    @Test
    void givenCustomizedConnector_whenTcpipIsRestarted_thenRebind() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (counter.getAndIncrement() == 0) {
                throw new IOException("EDC5122I");
            } else {
                return socketChannel;
            }
        }).when(serverSocket).accept();

        assertSame(socketChannel, testEndpoint.serverSocketAccept());
        verify(serverSocket, times(1)).implCloseSelectableChannel();
        verify(testEndpoint, times(1)).bind();
    }

    @Test
    void givenCustomizedConnector_whenTcpipIsRestarted_thenWaitForTcpIpAndRebind() throws Exception {
        AtomicInteger counterAccept = new AtomicInteger(0);
        AtomicInteger counterBind = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (counterAccept.getAndIncrement() == 0) {
                throw new IOException("EDC5122I TCP/IP stack was restarted");
            } else {
                return socketChannel;
            }
        }).when(serverSocket).accept();
        doAnswer(invocation -> {
            if (counterBind.getAndIncrement() < 2) {
                throw new IOException();
            } else {
                return 0;
            }
        }).when(testEndpoint).bind();

        assertSame(socketChannel, testEndpoint.serverSocketAccept());
        verify(serverSocket, times(1)).implCloseSelectableChannel();
        verify(testEndpoint, times(3)).bind();
    }

    @Test
    void givenCustomizedConnector_whenSocketCannotBeClosed_thenThrowException() throws Exception {
        doThrow(new IOException("EDC5122I Socket accept failed")).when(serverSocket).accept();
        doThrow(new IOException("EDC5122I Resource temporarily unavailable.")).when(serverSocket).close();

        assertThrows(IOException.class, testEndpoint::serverSocketAccept);
    }

    @Test
    void givenCustomizedConnector_whenAcceptFails_thenThrowException() throws IOException {
        IOException ioe = new IOException("Unexpected error");
        doThrow(ioe).when(serverSocket).accept();
        IOException ioe2 = assertThrows(IOException.class, testEndpoint::serverSocketAccept);
        assertSame(ioe, ioe2);
        verify(serverSocket, never()).implCloseSelectableChannel();
        verify(testEndpoint, never()).bind();
    }

    @Test
    void givenNonNioProtocol_whenCustomize_thenNotCustomized() {
        AjpNio2Protocol protocol = new AjpNio2Protocol();
        Connector connector = createCustomizer(protocol);
        fireEventStarted(connector, LifecycleState.STARTED);
        assertSame(protocol, connector.getProtocolHandler());
    }

    @Test
    void givenNonAbstractProtocol_whenCustomize_thenNotCustomized() {
        ProtocolHandler protocol = mock(ProtocolHandler.class);
        Connector connector = createCustomizer(protocol);
        fireEventStarted(connector, LifecycleState.STARTED);
        assertSame(protocol, connector.getProtocolHandler());
    }

    @Test
    void givenSupportedProtocol_whenNonStartedEvent_thenNotCustomized() {
        ProtocolHandler protocol = mock(ProtocolHandler.class);
        Connector connector = createCustomizer(protocol);
        fireEventStarted(connector, LifecycleState.STARTING);
        assertSame(protocol, connector.getProtocolHandler());
    }

    @Test
    void givenRestartedTcpipStack_whenServiceShutingDown_thenStop() throws IOException {
        doThrow(new IOException("EDC5122I Socket accept failed")).when(serverSocket).accept();
        doThrow(new IOException("EDC5122I Resource temporarily unavailable.")).when(testEndpoint).bind();
        tomcatAcceptFixConfig.stopping();
        assertThrows(IOException.class, testEndpoint::serverSocketAccept);
    }

    @Test
    void givenIoException_whenImplCloseSelectableChannel_thenIsPropagated() {
        ServerSocketChannel socket = new TestServerSocketChannel(null) {
            @Override
            public void implCloseSelectableChannel() throws IOException {
                throw new IOException("ioe message");
            }
        };
        TomcatAcceptFixConfig.FixedServerSocketChannel fixedServerSocketChannel = tomcatAcceptFixConfig.new FixedServerSocketChannel(socket, null, null);
        assertThrows(IOException.class, fixedServerSocketChannel::implCloseSelectableChannel);
    }

    @Test
    void givenRuntimeException_whenImplCloseSelectableChannel_thenIsPropagated() {
        ServerSocketChannel socket = new TestServerSocketChannel(null) {
            @Override
            public void implCloseSelectableChannel() {
                throw new IllegalStateException("IllegalStateException message");
            }
        };
        TomcatAcceptFixConfig.FixedServerSocketChannel fixedServerSocketChannel = tomcatAcceptFixConfig.new FixedServerSocketChannel(socket, null, null);
        assertThrows(IllegalStateException.class, fixedServerSocketChannel::implCloseSelectableChannel);
    }

    @Test
    void givenIoException_whenImplConfigureBlocking_thenIsPropagated() {
        ServerSocketChannel socket = new TestServerSocketChannel(null) {
            @Override
            public void implConfigureBlocking(boolean block) throws IOException {
                throw new IOException("ioe message");
            }
        };
        TomcatAcceptFixConfig.FixedServerSocketChannel fixedServerSocketChannel = tomcatAcceptFixConfig.new FixedServerSocketChannel(socket, null, null);
        assertThrows(IOException.class, () -> fixedServerSocketChannel.implConfigureBlocking(true));
    }

    @Test
    void givenRuntimeException_whenImplConfigureBlocking_thenIsPropagated() {
        ServerSocketChannel socket = new TestServerSocketChannel(null) {
            @Override
            public void implConfigureBlocking(boolean block) {
                throw new IllegalStateException("IllegalStateException message");
            }
        };
        TomcatAcceptFixConfig.FixedServerSocketChannel fixedServerSocketChannel = tomcatAcceptFixConfig.new FixedServerSocketChannel(socket, null, null);
        assertThrows(IllegalStateException.class, () -> fixedServerSocketChannel.implConfigureBlocking(true));
    }

    @Nested
    class TcpStackRestartHandling {

        ServerSocketChannel serverSocket = new TestServerSocketChannel(mock(SelectorProvider.class));
        TomcatAcceptFixConfig.FixedServerSocketChannel channel = new TomcatAcceptFixConfig().new FixedServerSocketChannel(serverSocket, null, null);

        @Test
        void givenExceptionWithTheMessage_whenHandle_thenReturnTrue() {
            assertTrue(channel.isTcpStackRestarted(new RuntimeException("EDC5122I TCP Stack restarted")));
        }

        @Test
        void givenExceptionWithCyclicCause_whenHandle_thenReturnFalse() {
            Exception e = spy(new RuntimeException("Error"));
            doReturn(e).when(e).getCause();
            assertFalse(channel.isTcpStackRestarted(e));
        }

        @Test
        void givenExceptionWithTheMessageAsCause_whenHandle_thenReturnTrue() {
            Exception e = new RuntimeException("EDC5122I TCP Stack restarted");
            e = new RuntimeException("Wrapper1", e);
            e = new RuntimeException("Wrapper2", e);
            assertTrue(channel.isTcpStackRestarted(e));
        }

        @Test
        void givenExceptionWithSpecificClassName_whenHandle_thenReturnTrue() {
            TomcatAcceptFixConfig.FixedServerSocketChannel channel = new TomcatAcceptFixConfig().new FixedServerSocketChannel(serverSocket, null, null) {
                @Override
                boolean isRecycledClass(Throwable t) {
                    return "java.lang.IllegalArgumentException".equals(t.getClass().getName());
                }
            };

            Exception e = new IllegalArgumentException("Tested exception");
            e = new RuntimeException("Wrapper", e);
            assertTrue(channel.isTcpStackRestarted(e));
        }

    }

    private static class TestEndpoint extends NioEndpoint {

        public TestEndpoint(ServerSocketChannel serverSocket) {
            setServerSocket(serverSocket);
        }

        public void setServerSocket(ServerSocketChannel serverSocket) {
            try {
                Field field = NioEndpoint.class.getDeclaredField("serverSock");
                field.setAccessible(true);
                field.set(this, serverSocket);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void bind() throws IOException { // Mockito throw IOException in the test
        }

        @Override
        public SocketChannel serverSocketAccept() throws Exception {
            return ((ServerSocketChannel) getServerSocket()).accept();
        }

    }

    private static class TestProtocol extends Http11NioProtocol {

        public <S, U> TestProtocol(AbstractEndpoint<S, U> endpoint) {
            try {
                Field endpointField = AbstractProtocol.class.getDeclaredField("endpoint");
                endpointField.setAccessible(true);
                endpointField.set(this, endpoint);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

    }

    private static class TestServerSocketChannel extends ServerSocketChannel {

        public TestServerSocketChannel(SelectorProvider provider) {
            super(provider);
        }

        @Override
        public void implCloseSelectableChannel() throws IOException { // Mockito throw IOException in the test

        }

        @Override
        protected void implConfigureBlocking(boolean block) throws IOException {

        }

        @Override
        public ServerSocketChannel bind(SocketAddress local, int backlog) {
            return null;
        }

        @Override
        public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) {
            return null;
        }

        @Override
        public <T> T getOption(SocketOption<T> name) {
            return null;
        }

        @Override
        public Set<SocketOption<?>> supportedOptions() {
            return null;
        }

        @Override
        public ServerSocket socket() {
            return null;
        }

        @Override
        public SocketChannel accept() throws IOException { // Mockito throw IOException in the test
            return null;
        }

        @Override
        public SocketAddress getLocalAddress() {
            return null;
        }

    }

}