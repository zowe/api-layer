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

import org.apache.tomcat.util.buf.ByteBufferUtils;
import org.apache.tomcat.util.http.parser.HttpParser;
import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.net.SecureNioChannel;
import org.apache.tomcat.util.net.SocketBufferHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Reads only a bounded HTTP request line before returning a redirect. Plaintext never
 * completes the handshake or enters Tomcat's application request processing.
 * TLS bytes remain in netInBuffer for the superclass to consume.
 */
class HttpsRedirectChannel extends SecureNioChannel {

    private enum State { DETECT, TLS, HTTP, RESPONSE }

    private final String origin;
    private final int requestLimit;
    private final long timeoutMillis;
    private State state = State.DETECT;
    private ByteBuffer response;
    private int scanned;
    private int lineStart;
    private long started;

    HttpsRedirectChannel(SocketBufferHandler buffer, NioEndpoint endpoint, String origin, int requestLimit) {
        super(buffer, endpoint);
        this.origin = origin;
        this.requestLimit = requestLimit;
        timeoutMillis = endpoint.getConnectionTimeout() > 0 ? endpoint.getConnectionTimeout() : 60000;
    }

    @Override
    public void reset(SocketChannel channel, NioEndpoint.NioSocketWrapper wrapper) throws IOException {
        super.reset(channel, wrapper);
        state = State.DETECT;
        response = null;
        scanned = 0;
        lineStart = 0;
        started = System.nanoTime();
    }

    @Override
    public int handshake(boolean read, boolean write) throws IOException {
        if (state == State.TLS) {
            return super.handshake(read, write);
        }
        if (state == State.RESPONSE) {
            return writeResponse();
        }
        if (!read) {
            return SelectionKey.OP_READ;
        }
        if (!netInBuffer.hasRemaining()) {
            int capacity = Math.min(requestLimit, netInBuffer.capacity() * 2);
            if (capacity <= netInBuffer.capacity()) {
                return respond("414 URI Too Long", null);
            }
            netInBuffer = ByteBufferUtils.expand(netInBuffer, capacity);
        }
        int count = sc.read(netInBuffer);
        if (count == -1) {
            return -1;
        }
        if (netInBuffer.position() == 0) {
            return SelectionKey.OP_READ;
        }
        if (state == State.DETECT) {
            int first = netInBuffer.get(0) & 0xff;
            // All HTTP method tokens are ASCII. Binary input stays on Tomcat's TLS path.
            if (!HttpParser.isToken(first) && first != '\r' && first != '\n') {
                state = State.TLS;
                return super.handshake(read, write);
            }
            state = State.HTTP;
        }
        long remaining = timeoutMillis - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        if (remaining <= 0) {
            return -1;
        }
        // A client sending one byte at a time must not extend the deadline indefinitely.
        socketWrapper.setReadTimeout(remaining);
        int limit = Math.min(netInBuffer.position(), requestLimit);
        while (scanned < limit) {
            if (netInBuffer.get(scanned++) == '\n') {
                if (scanned - lineStart == 2 && netInBuffer.get(lineStart) == '\r') {
                    lineStart = scanned;
                    continue;
                }
                try {
                    String line = StandardCharsets.US_ASCII.decode(
                        netInBuffer.asReadOnlyBuffer().flip().limit(scanned).position(lineStart)).toString();
                    return respond("308 Permanent Redirect", origin + requestTarget(line));
                } catch (IllegalArgumentException e) {
                    return respond("400 Bad Request", null);
                }
            }
        }
        if (netInBuffer.position() >= requestLimit) {
            return respond("414 URI Too Long", null);
        }
        return SelectionKey.OP_READ;
    }

    private int respond(String status, String location) throws IOException {
        String headers = "HTTP/1.1 " + status + "\r\n"
            + (location == null ? "" : "Location: " + location + "\r\n")
            + "Content-Length: 0\r\nConnection: close\r\n\r\n";
        response = ByteBuffer.wrap(headers.getBytes(StandardCharsets.US_ASCII));
        state = State.RESPONSE;
        return writeResponse();
    }

    private int writeResponse() throws IOException {
        // Keep OP_WRITE registered until the entire response is sent, then let Tomcat close.
        sc.write(response);
        return response.hasRemaining() ? SelectionKey.OP_WRITE : -1;
    }

    static String requestTarget(String line) {
        if (!line.endsWith("\r\n")) {
            throw new IllegalArgumentException("Invalid HTTP request line");
        }
        String[] parts = line.substring(0, line.length() - 2).split(" ", -1);
        if (parts.length != 3 || parts[0].isEmpty()
            || !parts[0].chars().allMatch(HttpParser::isToken)
            || (!"HTTP/1.1".equals(parts[2]) && !"HTTP/1.0".equals(parts[2]))
            || "CONNECT".equals(parts[0])) {
            throw new IllegalArgumentException("Invalid HTTP request line");
        }
        String target = parts[1];
        if (target.isEmpty() || target.chars().anyMatch(c -> c <= 32 || c >= 127 || c == '\\' || c == '#')) {
            throw new IllegalArgumentException("Invalid HTTP request target");
        }
        if ("*".equals(target) && "OPTIONS".equals(parts[0])) {
            return "/";
        }
        URI uri = URI.create(target);
        if (!target.startsWith("/")) {
            if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getRawUserInfo() != null) {
                throw new IllegalArgumentException("Invalid absolute HTTP request target");
            }
            String path = uri.getRawPath();
            return (path.isEmpty() ? "/" : path) + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        }
        // Concatenation (not URI.resolve) keeps // paths from replacing the configured authority.
        return target;
    }
}
