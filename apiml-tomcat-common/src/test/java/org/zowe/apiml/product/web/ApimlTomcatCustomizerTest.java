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

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.commons.attls.AttlsContext;
import org.zowe.commons.attls.ContextIsNotInitializedException;
import org.zowe.commons.attls.InboundAttls;

import java.io.FileDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

class ApimlTomcatCustomizerTest {

    private Connector connector = new Connector();
    private AbstractEndpoint<Object, ?> endpoint;
    private AbstractEndpoint.Handler<Object> originalHandler, handler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws LifecycleException {
        endpoint = (AbstractEndpoint<Object, ?>) ReflectionTestUtils.getField(connector.getProtocolHandler(), "endpoint");
        originalHandler = (AbstractEndpoint.Handler<Object>) ReflectionTestUtils.getField(endpoint, "handler");
        originalHandler = spy(originalHandler);

        ReflectionTestUtils.setField(endpoint, "handler", originalHandler);
        ReflectionTestUtils.setField(connector.getProtocolHandler(), "handler", originalHandler);

        endpoint.setBindOnInit(false);
        connector.init();

        new ApimlTomcatCustomizer<>().customize(connector);

        handler = spy((AbstractEndpoint.Handler<Object>) ReflectionTestUtils.getField(endpoint, "handler"));
        ReflectionTestUtils.setField(endpoint, "handler", handler);
    }

    @Test
    void providedCorrectProtocolInConnector_endpointIsConfigured() {
        ApimlTomcatCustomizer<?, ?> customizer = new ApimlTomcatCustomizer<>();
        customizer.afterPropertiesSet();
        Http11NioProtocol protocol = new Http11NioProtocol();
        Connector connector = new Connector(protocol);
        customizer.customize(connector);
        Http11NioProtocol protocolHandler = (Http11NioProtocol) connector.getProtocolHandler();
        AbstractEndpoint<?, ?> abstractEndpoint = ReflectionTestUtils.invokeMethod(protocolHandler, "getEndpoint");
        assumeTrue(abstractEndpoint != null);
        assertEquals(ApimlTomcatCustomizer.ApimlAttlsHandler.class, abstractEndpoint.getHandler().getClass());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void whenSocketArrives_fileDescriptorIsObtained() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ContextIsNotInitializedException {
        AbstractEndpoint.Handler handler = mock(AbstractEndpoint.Handler.class);
        NioChannel socket = mock(NioChannel.class);

        Constructor<SocketChannel> channelConstructor = (Constructor<SocketChannel>) Class.forName("sun.nio.ch.SocketChannelImpl").getDeclaredConstructor(SelectorProvider.class);
        channelConstructor.setAccessible(true);
        SelectorProvider selectorProvider = mock(SelectorProvider.class);
        SocketChannel socketChannel = channelConstructor.newInstance(selectorProvider);
        socketChannel.socket().getLocalPort();
        ApimlTomcatCustomizer.ApimlAttlsHandler apimlAttlsHandler = new ApimlTomcatCustomizer.ApimlAttlsHandler(handler);

        when(socket.getIOChannel()).thenReturn(socketChannel);
        SocketWrapperBase<Object> socketWrapperBase = new SocketWrapperBaseTest(socket, endpoint);

        doAnswer(answer -> {
            int fdNumber = (int) ReflectionTestUtils.getField(InboundAttls.get(), "id");
            int port = getFd(socketChannel);
            assertEquals(port, fdNumber);
            return AbstractEndpoint.Handler.SocketState.OPEN;
        }).when(handler).process(socketWrapperBase, SocketEvent.OPEN_READ);

        try {
            ThreadLocal<AttlsContext> mockThreadLocal = mock(ThreadLocal.class);
            AtomicReference<AttlsContext> attlsContextHolder = new AtomicReference<>();
            doAnswer(answer -> {
                AttlsContext attlsContext = answer.getArgument(0);
                int fd = (int) ReflectionTestUtils.getField(attlsContext, "id");
                attlsContextHolder.set(spy(new AttlsContext(fd, false) {
                    @Override
                    public void clean() {}
                }));
                return null;
            }).when(mockThreadLocal).set(any());
            doAnswer(answer -> attlsContextHolder.get()).when(mockThreadLocal).get();
            ReflectionTestUtils.setField(InboundAttls.class, "contexts", mockThreadLocal);

            assertEquals(AbstractEndpoint.Handler.SocketState.OPEN, apimlAttlsHandler.process(socketWrapperBase, SocketEvent.OPEN_READ));
            verify(attlsContextHolder.get()).clean();
        } finally {
            ReflectionTestUtils.setField(InboundAttls.class, "contexts", new ThreadLocal());
        }
    }

    @Test
    void givenConnector_whenNio2ProtocolIsUsed_thenContextIsCreated()
        throws ClassNotFoundException, NoSuchFieldException
    {
        // example of file descriptor
        FileDescriptor fd = createFileDescriptor(98741);

        // verify using class and required field
        Field fdField = Class.forName("sun.nio.ch.AsynchronousSocketChannelImpl").getDeclaredField("fd");
        assertSame(FileDescriptor.class, fdField.getType());

        /**
         * Original class cannot be instantiated. Right child depends on OS and their parent is abstract and final.
         * For this reason, the right type is replaced with classes to test and verify structure and process.
         */
        ReflectionTestUtils.setField(ApimlTomcatCustomizer.ApimlAttlsHandler.class, "ASYNCHRONOUS_SOCKET_CHANNEL_FD",
            AsynchronousSocketChannelTest.class.getDeclaredField("fd"));

        // prepare parameters in awaited structure
        AsynchronousSocketChannel sc = new AsynchronousSocketChannelTest(fd);
        Nio2Channel nio2Channel = new Nio2Channel(null);
        ReflectionTestUtils.setField(nio2Channel, "sc", sc);

        when(nio2Channel.getIOChannel()).thenReturn(sc);
        SocketWrapperBase<Object> socketWrapperBase = new SocketWrapperBaseTest(nio2Channel, endpoint);
        doAnswer(answer -> {
            int fdNumber = (int) ReflectionTestUtils.getField(InboundAttls.get(), "id");
            int port = getFd(sc);
            assertEquals(port, fdNumber);
            return AbstractEndpoint.Handler.SocketState.OPEN;
        }).when(handler).process(socketWrapperBase, SocketEvent.OPEN_READ);

        try {
            ThreadLocal<AttlsContext> mockThreadLocal = mock(ThreadLocal.class);
            AtomicReference<AttlsContext> attlsContextHolder = new AtomicReference<>();
            doAnswer(answer -> {
                AttlsContext attlsContext = answer.getArgument(0);
                int fd1 = (int) ReflectionTestUtils.getField(attlsContext, "id");
                attlsContextHolder.set(spy(new AttlsContext(fd1, false) {
                    @Override
                    public void clean() {}
                }));
                return null;
            }).when(mockThreadLocal).set(any());
            doAnswer(answer -> attlsContextHolder.get()).when(mockThreadLocal).get();
            ReflectionTestUtils.setField(InboundAttls.class, "contexts", mockThreadLocal);
            doAnswer(answer -> (Answer<AbstractEndpoint.Handler.SocketState>) invocation -> {
                assertNotNull(InboundAttls.get());
                Integer id = (Integer) ReflectionTestUtils.getField(InboundAttls.get(), "id");
                assertNotNull(id);
                return AbstractEndpoint.Handler.SocketState.OPEN;
            }).when(originalHandler).process(any(), any());

        } finally {
            ReflectionTestUtils.setField(InboundAttls.class, "contexts", new ThreadLocal());
        }
       assertContextIsClean();
    }

    private int getFd(SocketChannel socketChannel) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        Method getFDMethod = socketChannel.getClass().getDeclaredMethod("getFD");
        getFDMethod.setAccessible(true);
        FileDescriptor fd = (FileDescriptor) getFDMethod.invoke(socketChannel);
        Field fdField = FileDescriptor.class.getDeclaredField("fd");
        fdField.setAccessible(true);
        int port = (int) fdField.get(fd);
        return port;
    }

    private int getFd(AsynchronousSocketChannel socketChannel) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        Method getFDMethod = socketChannel.getClass().getDeclaredMethod("getFD");
        getFDMethod.setAccessible(true);
        FileDescriptor fd = (FileDescriptor) getFDMethod.invoke(socketChannel);
        Field fdField = FileDescriptor.class.getDeclaredField("fd");
        fdField.setAccessible(true);
        int port = (int) fdField.get(fd);
        return port;
    }

    private FileDescriptor createFileDescriptor(int fd) {
        FileDescriptor out = new FileDescriptor();
        ReflectionTestUtils.setField(out, "fd", fd);
        return out;
    }

    private void assertContextIsClean() {
        try {
            InboundAttls.get();
            fail();
        } catch (ContextIsNotInitializedException e) {
            System.out.println("clean");
            // exception means context is clean, it does not exist
        }
    }
    private static class AsynchronousSocketChannelTest extends AsynchronousSocketChannel {

        @SuppressWarnings("unused")
        public FileDescriptor fd;

        public AsynchronousSocketChannelTest(FileDescriptor fd) {
            super(null);
            this.fd = fd;
        }

        @Override
        public AsynchronousSocketChannel bind(SocketAddress local) {
            return null;
        }

        @Override
        public <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) {
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
        public AsynchronousSocketChannel shutdownInput() {
            return null;
        }

        @Override
        public AsynchronousSocketChannel shutdownOutput() {
            return null;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
        }

        @Override
        public Future<Void> connect(SocketAddress remote) {
            return null;
        }

        @Override
        public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        }

        @Override
        public Future<Integer> read(ByteBuffer dst) {
            return null;
        }

        @Override
        public <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        }

        @Override
        public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        }

        @Override
        public Future<Integer> write(ByteBuffer src) {
            return null;
        }

        @Override
        public <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        }

        @Override
        public SocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public void close() {
        }

    }

    private static class SocketWrapperBaseTest extends SocketWrapperBase<Object> {

        public SocketWrapperBaseTest(Object socket, AbstractEndpoint<Object,?> endpoint) {
            super(socket, endpoint);
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
        public int read(boolean block, byte[] b, int off, int len) {
            return 0;
        }

        @Override
        public int read(boolean block, ByteBuffer to) {
            return 0;
        }

        @Override
        public boolean isReadyForRead() {
            return false;
        }

        @Override
        public void setAppReadBufHandler(ApplicationBufferHandler handler) {
        }

        @Override
        protected void doClose() {
        }

        @Override
        protected boolean flushNonBlocking() {
            return false;
        }

        @Override
        protected void doWrite(boolean block, ByteBuffer from) {
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
        public void doClientAuth(SSLSupport sslSupport) {
        }

        @Override
        public SSLSupport getSslSupport() {
            return null;
        }

        @Override
        protected <A> SocketWrapperBase<Object>.OperationState<A> newOperationState(boolean read, ByteBuffer[] buffers, int offset, int length, BlockingMode block, long timeout, TimeUnit unit, A attachment, CompletionCheck check, CompletionHandler<Long, ? super A> handler, Semaphore semaphore, SocketWrapperBase<Object>.VectoredIOCompletionHandler<A> completion) {
            return null;
        }

    }

}
