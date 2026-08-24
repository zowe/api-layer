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

import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.NioEndpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class extends embedded Tomcat to handle a case with restarting TCP/IP stack. Tomcat itself is not able
 * recovery from this situation. It tries accepting new request, and it fails until the service is restarted.
 *
 * This handler detects this issue and try to rebind the port. It can also wait till the TCP/IP stack is available again.
 *
 * This bean is related only to z/OS.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class TomcatAcceptFixConfig {

    @Value("${server.tomcat.retryRebindTimeoutSecs:10}")
    int retryRebindTimeoutSecs;

    @Value("${apiml.tcpStackAwareSocketChannel.enabled:false}")
    boolean tcpStackAwareSocketChannelEnabled;

    private static final Field ENDPOINT_FIELD;
    private static final Field NIO_SOCKET_FIELD;

    private static final MethodHandle IMPL_CLOSE_SELECTABLE_CHANNEL_HANDLE; // NOSONAR
    private static final MethodHandle IMPL_CONFIGURE_BLOCKING; // NOSONAR
    private static final String NETWORK_RECYCLED_EXCEPTION_CLASS = "com.ibm.net.NetworkRecycledException";

    /**
     * To mitigate parallel treatment of socket rebinding
     */
    private static final AtomicBoolean running = new AtomicBoolean(true);

    static {
        try {
            ENDPOINT_FIELD = AbstractProtocol.class.getDeclaredField("endpoint");
            NIO_SOCKET_FIELD = NioEndpoint.class.getDeclaredField("serverSock");

            Method implCloseSelectableChannel = AbstractSelectableChannel.class.getDeclaredMethod("implCloseSelectableChannel");
            implCloseSelectableChannel.setAccessible(true); // NOSONAR
            IMPL_CLOSE_SELECTABLE_CHANNEL_HANDLE = MethodHandles.lookup().unreflect(implCloseSelectableChannel);

            Method implConfigureBlocking = AbstractSelectableChannel.class.getDeclaredMethod("implConfigureBlocking", boolean.class);
            implConfigureBlocking.setAccessible(true); // NOSONAR
            IMPL_CONFIGURE_BLOCKING = MethodHandles.lookup().unreflect(implConfigureBlocking);
        } catch (NoSuchFieldException | NoSuchMethodException | SecurityException | IllegalAccessException e) {
            throw new IllegalStateException("Unknown structure of protocols", e);
        }
        ENDPOINT_FIELD.setAccessible(true); // NOSONAR
        NIO_SOCKET_FIELD.setAccessible(true); // NOSONAR
    }

    /**
     * Update Protocol class of Tomcat. For supported protocols (Nio) it will replace endpoint implementation
     * with the wrapper class providing the fix - handling of restart of TCP/IP stack.
     *
     * @param abstractProtocol instance to update
     * @param rebindHandler call-back (to next update after TCP/IP restart)
     */
    private void update(AbstractProtocol<?> abstractProtocol, Runnable rebindHandler) {
        try {
            AbstractEndpoint<?, ?> abstractEndpoint = (AbstractEndpoint<Object, Object>) ENDPOINT_FIELD.get(abstractProtocol);

            if (abstractEndpoint instanceof NioEndpoint) {
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) NIO_SOCKET_FIELD.get(abstractEndpoint);
                serverSocketChannel = new FixedServerSocketChannel(serverSocketChannel, abstractEndpoint, rebindHandler);
                NIO_SOCKET_FIELD.set(abstractEndpoint, serverSocketChannel); // NOSONAR
            } else {
                log.warn("Unsupported protocol: {}", abstractEndpoint.getClass().getName());
            }
        } catch (Exception e) {
            log.warn("Cannot update connector to handle errors on socket accepting", e);
        }
    }

    /**
     * Update protocol inside the protocol. This method is on the highest level to allow updating each required part.
     *
     * @param connector connector to update
     *
     * Note: If protocol of connector is not supported no change happened.
     */
    private void update(Connector connector) {
        ProtocolHandler protocolHandler = connector.getProtocolHandler();
        if (protocolHandler instanceof AbstractProtocol) {
            update((AbstractProtocol<?>) protocolHandler, () -> update(connector));
        } else {
            log.warn("Unsupported protocol handler: {}", protocolHandler.getClass().getName());
        }
    }

    /**
     * @return customizer class for Embedded Tomcat to extend connectors. It will restart server socket in the case
     * of TCP/IP stack restart
     */
    @Bean
    public TomcatConnectorCustomizer tomcatAcceptorFix() {
        return connector -> connector.addLifecycleListener(event -> {
            if (event.getLifecycle().getState() == LifecycleState.STARTED) {
                update(connector);

                log.debug("TomcatAcceptFixConfig applied");
            }
        });
    }

    /**
     * Detection of service stopping. It will unblock threads to successful shutdown.
     */
    @PreDestroy
    public void stopping() {
        running.set(false);
    }

    static boolean isRecycledClass(Throwable t) {
        return NETWORK_RECYCLED_EXCEPTION_CLASS.equals(t.getClass().getName());
    }

    static boolean isTcpStackRestarted(Throwable t) {
        if ((t.getMessage() != null) && t.getMessage().contains("EDC5122I")) {
            return true;
        }

        if (isRecycledClass(t)) {
            return true;
        }

        Throwable cause = t.getCause();
        if ((cause != null) && (cause != t)) {
            return isTcpStackRestarted(cause);
        }

        return false;
    }

    /**
     * Socket implementation wrapper to handle rebinding on TCP Stack restart
     */
    class FixedServerSocketChannel extends ServerSocketChannel {


        /**
         * Wrapper server socket inside
         */
        @Delegate(excludes = Overridden.class)
        private final ServerSocketChannel socket;

        /**
         * The endpoint instance used by Server socket
         */
        private final AbstractEndpoint<?, ?> abstractEndpoint;

        /**
         * Define the generation of binding, each rebinding the value is increased
         */
        private final AtomicInteger state = new AtomicInteger();

        /**
         * Handler to call after successful rebind of server socket
         */
        private final Runnable rebindHandler;

        FixedServerSocketChannel(ServerSocketChannel socket, AbstractEndpoint<?, ?> abstractEndpoint, Runnable rebindHandler) {
            super(socket.provider());
            this.socket = socket;
            this.abstractEndpoint = abstractEndpoint;
            this.rebindHandler = rebindHandler;
        }

        @Override
        protected void implCloseSelectableChannel() throws IOException {
            try {
                IMPL_CLOSE_SELECTABLE_CHANNEL_HANDLE.invoke(socket);
            } catch (IOException | RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        }

        @Override
        protected void implConfigureBlocking(boolean block) throws IOException {
            try {
                IMPL_CONFIGURE_BLOCKING.invoke(socket, block);
            } catch (IOException | RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        }

        /**
         * Method tries to bind port. In case it is not available it waits until TCP/IP stack is restarted
         *
         * @throws IOException bind is not possible at the moment for an unspecific error
         * @throws InterruptedException if thread was interrupted. It cannot wait to next attempt of binding.
         */
        private void bindWithWait() throws IOException, InterruptedException {
            while (true) {
                try {
                    abstractEndpoint.bind();
                    break;
                } catch (Throwable t) {
                    log.debug("Cannot rebind socket", t);
                    if (!running.get()) {
                        throw new IOException("Application is stopping during the attempt to rebind the socket", t);
                    }
                    // delay between attempt to rebinding to avoid overloading
                    Thread.sleep(retryRebindTimeoutSecs);
                }
            }
        }

        /**
         * Rebind the server socket. The action could be done just by one thread. Other treats are waiting to be finish
         * by first one.
         *
         * @param stateBefore id of socket binding
         * @throws IOException unexpected exception during rebinding (socket cannot be closed or the connector cannot
         * be re-updated)
         */
        private synchronized void rebind(int stateBefore) throws IOException {
            if (state.compareAndSet(stateBefore, stateBefore + 1)) {
                try {
                    // socket must be closed before new binding
                    socket.close();

                    // till TCP/IP stack is running again try to bind the port
                    bindWithWait();

                    // in case of successfully update the connector instance, it is not handled by this wrapper anymore
                    rebindHandler.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    throw new IOException("Cannot rebind the port", e);
                }
            }
        }

        boolean isTcpStackRestarted(Throwable t) {
            return TomcatAcceptFixConfig.isTcpStackRestarted(t);
        }

        public SocketChannel accept() throws IOException {
            // obtain current state of rebinding to detection parallel actions
            final int stateBefore = state.get();
            try {
                return wrapIfEnabled(socket.accept());
            } catch (IOException ioe) {
                if (TomcatAcceptFixConfig.isTcpStackRestarted(ioe)) {
                    // the fix solve just one issue about stopped TCP/IP stack
                    log.debug("The TCP/IP stack was probably restarted. The socket of Tomcat will rebind.");
                    rebind(stateBefore);
                    return wrapIfEnabled(socket.accept());
                }
                throw ioe;
            }
        }

        private SocketChannel wrapIfEnabled(SocketChannel socketChannel) {
            if (tcpStackAwareSocketChannelEnabled) {
                return new TcpStackAwareSocketChannel(socketChannel);
            }
            return socketChannel;
        }

    }

    /**
     * The list of final methods, which cannot be delegated. See {@link FixedServerSocketChannel#socket}
     */
    private interface Overridden {

        SocketChannel accept() throws IOException;
        int validOps();
        ServerSocketChannel bind(SocketAddress local) throws IOException;
        SelectorProvider provider();
        boolean isRegistered();
        SelectionKey keyFor(Selector sel);
        SelectionKey register(Selector sel, int ops, Object att);
        void implCloseChannel() throws IOException;
        boolean isBlocking();
        Object blockingLock();
        SelectableChannel configureBlocking(boolean block) throws IOException;
        SelectionKey register(Selector sel, int ops) throws ClosedChannelException;
        void close() throws IOException;
        boolean isOpen();

    }

    /**
     * The list of methods excluded from Lombok @Delegate for TcpStackAwareSocketChannel.
     * It contains methods inherited from SocketChannel that must not be delegated and
     * read/write/close-related methods that are wrapped explicitly in this class.
     */
    private interface ExcludedSocketOps {

        SelectorProvider provider();
        boolean isRegistered();
        SelectionKey keyFor(Selector sel);
        SelectionKey register(Selector sel, int ops, Object att);
        SelectionKey register(Selector sel, int ops) throws ClosedChannelException;
        boolean isBlocking();
        Object blockingLock();
        SelectableChannel configureBlocking(boolean block) throws IOException;
        void implCloseChannel() throws IOException;
        void close() throws IOException;
        boolean isOpen();
        int validOps();
        long read(ByteBuffer[] dsts) throws IOException;
        long write(ByteBuffer[] srcs) throws IOException;
        int read(ByteBuffer dst) throws IOException;
        long read(ByteBuffer[] dsts, int offset, int length) throws IOException;
        int write(ByteBuffer src) throws IOException;
        long write(ByteBuffer[] srcs, int offset, int length) throws IOException;
        void implCloseSelectableChannel() throws IOException;
        void implConfigureBlocking(boolean block) throws IOException;

    }

    /**
     * SocketChannel wrapper that detects z/OS TCP/IP stack restarts during I/O operations.
     * When EDC5122I or NetworkRecycledException is caught on read/write, the underlying
     * socket is closed and the exception is re-thrown so Tomcat can discard the connection.
     */
    class TcpStackAwareSocketChannel extends SocketChannel {

        @Delegate(excludes = ExcludedSocketOps.class)
        private final SocketChannel delegate;

        TcpStackAwareSocketChannel(SocketChannel delegate) {
            super(delegate.provider());
            this.delegate = delegate;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            try {
                return delegate.read(dst);
            } catch (IOException e) {
                if (TomcatAcceptFixConfig.isTcpStackRestarted(e)) {
                    log.debug("TCP/IP stack restart detected during read on client socket; closing connection", e);
                    safeClose(delegate);
                }
                throw e;
            }
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            try {
                return delegate.read(dsts, offset, length);
            } catch (IOException e) {
                if (TomcatAcceptFixConfig.isTcpStackRestarted(e)) {
                    log.debug("TCP/IP stack restart detected during scatter read on client socket; closing connection", e);
                    safeClose(delegate);
                }
                throw e;
            }
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            try {
                return delegate.write(src);
            } catch (IOException e) {
                if (TomcatAcceptFixConfig.isTcpStackRestarted(e)) {
                    log.debug("TCP/IP stack restart detected during write on client socket; closing connection", e);
                    safeClose(delegate);
                }
                throw e;
            }
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            try {
                return delegate.write(srcs, offset, length);
            } catch (IOException e) {
                if (TomcatAcceptFixConfig.isTcpStackRestarted(e)) {
                    log.debug("TCP/IP stack restart detected during scatter write on client socket; closing connection", e);
                    safeClose(delegate);
                }
                throw e;
            }
        }

        @Override
        protected void implCloseSelectableChannel() throws IOException {
            // SocketChannel.close() is final; it reaches this override through
            // AbstractInterruptibleChannel.close().
            delegate.close();
        }

        @Override
        protected void implConfigureBlocking(boolean block) throws IOException {
            try {
                IMPL_CONFIGURE_BLOCKING.invoke(delegate, block);
            } catch (IOException | RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        }

        private static void safeClose(SocketChannel ch) {
            try {
                ch.close();
            } catch (IOException e) {
                log.trace("Best-effort close of client socket failed", e);
            }
        }

    }

}
