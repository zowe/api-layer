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
import org.apache.tomcat.util.net.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.commons.attls.AttlsContext;
import org.zowe.commons.attls.ContextIsNotInitializedException;
import org.zowe.commons.attls.InboundAttls;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.MockUtil.isSpy;

class ApimlTomcatCustomizerTest {

    private Connector connector = new Connector();
    private AbstractEndpoint<Object, ?> endpoint;
    private AbstractEndpoint.Handler<Object> originalHandler, handler;

    private AttlsContext attlsContext;

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

        new ApimlTomcatCustomizer().customize(connector);

        handler = (AbstractEndpoint.Handler<Object>) ReflectionTestUtils.getField(endpoint, "handler");

        ThreadLocal<AttlsContext> contexts = spy((ThreadLocal<AttlsContext>) ReflectionTestUtils.getField(InboundAttls.class, "contexts"));
        ReflectionTestUtils.setField(InboundAttls.class, "contexts", contexts);
        doAnswer(answer -> {
            if (isSpy(answer.getArgument(0))) {
                return answer.callRealMethod();
            }

            attlsContext = answer.getArgument(0);
            attlsContext = spy(new AttlsContext(
                (int) ReflectionTestUtils.getField(attlsContext, "id"),
                (boolean) ReflectionTestUtils.getField(attlsContext, "alwaysLoadCertificate")
            ) {
                @Override
                public void clean() {
                }
            });
            contexts.set(attlsContext);
            return null;
        }).when(contexts).set(any());
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(InboundAttls.class, "contexts", new ThreadLocal<AttlsContext>());
    }

    private void mockOriginalHandlerAnswer(int fd) {
        doAnswer((Answer<AbstractEndpoint.Handler.SocketState>) invocation -> {
            assertNotNull(InboundAttls.get());
            Integer id = (Integer) ReflectionTestUtils.getField(InboundAttls.get(), "id");
            assertNotNull(id);
            if (id != fd) return null;
            return AbstractEndpoint.Handler.SocketState.OPEN;
        }).when(originalHandler).process(any(), any());
    }

    private FileDescriptor createFileDescriptor(int fd) {
        FileDescriptor out = new FileDescriptor();
        ReflectionTestUtils.setField(out, "fd", fd);
        return out;
    }

    private void assertContextIsClean() {
        if (attlsContext != null) {
            verify(attlsContext).clean();
        }

        try {
            InboundAttls.get();
            fail();
        } catch (ContextIsNotInitializedException e) {
            // exception means context is clean, it does not exist
        }
    }

    @Nested
    class ProtocolTests {

        @Test
        @SuppressWarnings("unchecked")
        void givenConnector_whenNioProtocolIsUsed_thenContextIsCreated()
                throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
                InvocationTargetException, InstantiationException, IOException {
            // example of file descriptor
            FileDescriptor fd = null;

            SocketChannel sc = null;

            try (ServerSocket server = new ServerSocket(0)) {
                new Thread(() -> {
                    try (Socket s = new Socket("127.0.0.1", server.getLocalPort())) {
                        // do nothing
                    } catch (Exception e) {
                        fail(e); // NOSONAR
                    }
                }).start();
                Socket socket = server.accept();
                SocketImpl impl = (SocketImpl) ReflectionTestUtils.getField(socket, "impl");
                fd = (FileDescriptor) ReflectionTestUtils.getField(impl, "fd");
                // prepare parameters in awaited structure
                Constructor<SocketChannel> constructor = ((Class<SocketChannel>) Class.forName("sun.nio.ch.SocketChannelImpl"))
                        .getDeclaredConstructor(SelectorProvider.class, ProtocolFamily.class, FileDescriptor.class, SocketAddress.class);
                constructor.setAccessible(true);
                sc = constructor.newInstance(null, StandardProtocolFamily.INET, fd, UnixDomainSocketAddress.of("id"));
            } catch (SecurityException | IllegalArgumentException e) {
                fail("Could not get socket", e);
            }

            NioChannel nioChannel = new NioChannel(null);
            ReflectionTestUtils.setField(nioChannel, "sc", sc);

            SocketWrapperBase<Object> socketWrapperBase = new SocketWrapperBaseTest(nioChannel, endpoint);

            mockOriginalHandlerAnswer((int) ReflectionTestUtils.getField(fd, FileDescriptor.class, "fd"));

            assertEquals(AbstractEndpoint.Handler.SocketState.OPEN, handler.process(socketWrapperBase, null));
            assertContextIsClean();
        }

        @Test
        void givenConnector_whenNio2ProtocolIsUsed_thenContextIsCreated()
                throws ClassNotFoundException, NoSuchFieldException {
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
            SocketWrapperBase<Object> socketWrapperBase = new SocketWrapperBaseTest(nio2Channel, endpoint);

            mockOriginalHandlerAnswer(98741);

            assertEquals(AbstractEndpoint.Handler.SocketState.OPEN, handler.process(socketWrapperBase, null));
            assertContextIsClean();
        }

        @Test
        void givenConnector_whenAprProtocolIsUsed_thenContextIsCreated() {
            SocketWrapperBase<Object> socketWrapperBase = new SocketWrapperBaseTest(258963L, endpoint);

            mockOriginalHandlerAnswer(258963);

            assertEquals(AbstractEndpoint.Handler.SocketState.OPEN, handler.process(socketWrapperBase, null));
            assertContextIsClean();
        }

        @Test
        void givenConnector_whenUnknownSocketTypeUsed_thenFailed() {
            SocketWrapperBase<Object> socketWrapperBase = new SocketWrapperBaseTest("unknown type", endpoint);

            try {
                handler.process(socketWrapperBase, null);
                fail();
            } catch (IllegalStateException e) {
                assertTrue(e.getMessage().contains("ATTLS-Incompatible configuration. Verify ATTLS requirements"), "Exception message was: " + e.getMessage());
                assertTrue(e.getCause().getMessage().contains("is not supported"), "Exception message was: " + e.getMessage());
            }
            assertContextIsClean();
        }

    }

    @Nested
    class ExceptionHandling {

        @Test
        void givenNioChannelWithoutIOChannel_whenGetFd_thenThrowException() {
            ApimlTomcatCustomizer.ApimlAttlsHandler<Object> customizer = new ApimlTomcatCustomizer.ApimlAttlsHandler<>(handler);
            NioChannel socket = mock(NioChannel.class);
            assertThrows(IllegalStateException.class, () -> customizer.getFd(socket));
        }

        @Test
        void givenNio2ChannelWithoutIOChannel_whenGetFdAsync_thenThrowException() {
            ApimlTomcatCustomizer.ApimlAttlsHandler<Object> customizer = new ApimlTomcatCustomizer.ApimlAttlsHandler<>(handler);
            Nio2Channel socket = mock(Nio2Channel.class);
            var ise = assertThrows(IllegalStateException.class, () -> customizer.getFdAsync(socket));
            assertTrue(ise.getMessage().contains("Asynchronous socket channel is not initialized"));
        }

        @Test
        void givenNio2ChannelWithoutFd_whenGetFdAsync_thenThrowException() throws NoSuchFieldException {
            ApimlTomcatCustomizer.ApimlAttlsHandler<Object> customizer = new ApimlTomcatCustomizer.ApimlAttlsHandler<>(handler);
            Nio2Channel socket = mock(Nio2Channel.class);
            AsynchronousSocketChannel sc = new AsynchronousSocketChannelTest(null);
            doReturn(sc).when(socket).getIOChannel();

            ReflectionTestUtils.setField(customizer, "ASYNCHRONOUS_SOCKET_CHANNEL_FD", AsynchronousSocketChannelTest.class.getDeclaredField("fd"));

            var ise = assertThrows(IllegalStateException.class, () -> customizer.getFdAsync(socket));
            assertTrue(ise.getMessage().contains("File descriptor is not set"));
        }

    }

    @Nested
    class Initialization {

        @Test
        void givenApimlTomcatCustomizerBean_whenInitialized_thenSetAlwaysLoadCertificate() throws ContextIsNotInitializedException {
            InboundAttls.setAlwaysLoadCertificate(false);
            new ApimlTomcatCustomizer().afterPropertiesSet();
            InboundAttls.init(0);
            assertTrue((Boolean) ReflectionTestUtils.getField(InboundAttls.get(), "alwaysLoadCertificate"));
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
