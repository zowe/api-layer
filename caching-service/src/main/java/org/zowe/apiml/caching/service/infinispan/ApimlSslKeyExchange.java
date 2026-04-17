/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.infinispan;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.protocols.SSL_KEY_EXCHANGE;
import org.jgroups.stack.IpAddress;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ApimlSslKeyExchange extends SSL_KEY_EXCHANGE {

    private static final ThreadLocal<List<Throwable>> EXCEPTIONS = new ThreadLocal<>();

    private static void addException(Exception e) {
        var exceptionList = EXCEPTIONS.get();
        if (exceptionList == null) {
            exceptionList = new ArrayList<>();
        }
        exceptionList.add(e);
        EXCEPTIONS.set(exceptionList);
    }

    String toString(Throwable t) {
        var stack = new ArrayList<Throwable>();
        Throwable previous;
        do {
            stack.add(t);
            previous = t;
            t = t.getCause();
        } while ((t != null) && (t != previous));
        return stack.stream().map(Throwable::toString).collect(Collectors.joining(": "));
    }

    void printError(String message, List<Throwable> exceptionList) {
        log.error("{}: {}", message, exceptionList.stream().map(this::toString).collect(Collectors.joining(", ")));
    }

    void printError(String message) {
        var exceptionList = EXCEPTIONS.get();
        if (exceptionList != null) {
            printError(message, exceptionList);
            EXCEPTIONS.remove();
        }
    }

    void decorate(Exception e) {
        var exceptionList = EXCEPTIONS.get();
        if (exceptionList != null) {
            var iterator = exceptionList.iterator();
            if ((e.getCause() == null) || (e.getCause() == e)) {
                e.initCause(iterator.next());
            }
            while (iterator.hasNext()) {
                e.addSuppressed(iterator.next());
            }
        }
    }

    protected SSLServerSocket createServerSocket() throws Exception {
        try {
            return super.createServerSocket();
        } catch (Exception e) {
            decorate(e);
            throw e;
        } finally {
            printError("Cannot create server socket");
        }
    }

    @Override
    protected SSLSocket createSocketTo(Address target) throws Exception {
        try {
            return super.createSocketTo(target);
        } catch (Exception e) {
            decorate(e);
            throw e;
        } finally {
            printError("Cannot create socket to remote address");
        }
    }

    @Override
    protected SSLSocket createSocketTo(IpAddress dest, SSLSocketFactory sslSocketFactory) {
        try {
            return super.createSocketTo(dest, sslSocketFactory);
        } catch (RuntimeException re) {
            decorate(re);
            throw re;
        } finally {
            printError("Cannot create socket to remote address");
        }
    }

    private SSLContext update(SSLContext context) {
        return new SSLContextWrapper(
            new SSLContextSpiWrapper(
                null,
                new SSLSocketFactoryWrapper(context.getSocketFactory()),
                new SSLServerSocketFactoryWrapper(context.getServerSocketFactory())
            ),
            context
        );
    }

    @Override
    public void init() throws Exception {
        synchronized (ApimlSslKeyExchange.class) {
            boolean update = (client_ssl_ctx == null || server_ssl_ctx == null);
            super.init();
            if (update) {
                super.client_ssl_ctx = update(super.client_ssl_ctx);
                super.server_ssl_ctx = update(super.server_ssl_ctx);
            }
        }
    }

    @Override
    public SSL_KEY_EXCHANGE setClientSSLContext(SSLContext client_ssl_ctx) {
        return super.setClientSSLContext(update(client_ssl_ctx));
    }

    @Override
    public SSL_KEY_EXCHANGE setServerSSLContext(SSLContext server_ssl_ctx) {
        return super.setServerSSLContext(update(server_ssl_ctx));
    }

    @RequiredArgsConstructor
    static class SSLSocketFactoryWrapper extends SSLSocketFactory {

        @Delegate
        private final SSLSocketFactory original;

        @Override
        public String[] getDefaultCipherSuites() {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public String[] getSupportedCipherSuites() {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            try {
                return original.createSocket(host, port);
            } catch (IOException | RuntimeException e) {
                addException(e);
                throw e;
            }
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            throw new IllegalStateException("Not implemented");
        }

    }

    @RequiredArgsConstructor
    static class SSLServerSocketFactoryWrapper extends SSLServerSocketFactory {

        @Delegate
        private final SSLServerSocketFactory original;

        @Override
        public String[] getDefaultCipherSuites() {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public String[] getSupportedCipherSuites() {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public ServerSocket createServerSocket(int port) throws IOException {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public ServerSocket createServerSocket(int port, int backlog) throws IOException {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
            try {
                return original.createServerSocket(port, backlog, ifAddress);
            } catch (IOException | RuntimeException e) {
                addException(e);
                throw e;
            }
        }

    }

    @RequiredArgsConstructor
    static class SSLContextSpiWrapper extends SSLContextSpi {

        @Delegate
        private final SSLContextSpi original;
        private final SSLSocketFactory sslSocketFactory;
        private final SSLServerSocketFactory sslServerSocketFactory;

        @Override
        protected void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr) throws KeyManagementException {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        protected SSLSocketFactory engineGetSocketFactory() {
            return this.sslSocketFactory;
        }

        @Override
        protected SSLServerSocketFactory engineGetServerSocketFactory() {
            return this.sslServerSocketFactory;
        }

        @Override
        protected SSLEngine engineCreateSSLEngine() {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        protected SSLEngine engineCreateSSLEngine(String host, int port) {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        protected SSLSessionContext engineGetServerSessionContext() {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        protected SSLSessionContext engineGetClientSessionContext() {
            throw new IllegalStateException("Not implemented");
        }

    }

    static class SSLContextWrapper extends SSLContext {

        SSLContextWrapper(SSLContextSpi contextSpi, SSLContext original) {
            super(contextSpi, original.getProvider(), original.getProtocol());
        }

    }

}
