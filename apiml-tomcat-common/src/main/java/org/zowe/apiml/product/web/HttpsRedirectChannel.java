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

    private enum State { DETECT, TLS, HTTP, RESPONSE, DRAIN }

    private final String origin;
    private final int requestLimit;
    private final HttpParser parser;
    private final long timeoutMillis;
    private State state = State.DETECT;
    private ByteBuffer response;
    private int scanned;
    private int lineStart;
    private long started;
    private long drainStarted;

    HttpsRedirectChannel(SocketBufferHandler buffer, NioEndpoint endpoint, String origin, int requestLimit, HttpParser parser) {
        super(buffer, endpoint);
        this.origin = origin;
        this.requestLimit = requestLimit;
        this.parser = parser;
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
        drainStarted = 0;
    }

    @Override
    public int handshake(boolean read, boolean write) throws IOException {
        if (state == State.TLS) {
            return super.handshake(read, write);
        }
        if (state == State.RESPONSE) {
            return writeResponse();
        }
        if (state == State.DRAIN) {
            return drainRequest();
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
            // TLS ClientHello records start with 0x16; retain Tomcat's handling of legacy SSL framing.
            if (first == 0x16 || first >= 0x80) {
                state = State.TLS;
                return super.handshake(read, write);
            }
            if (!HttpParser.isToken(first) && first != '\r' && first != '\n') {
                return respond("400 Bad Request", null);
            }
            state = State.HTTP;
        }
        long remaining = timeoutMillis - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        if (remaining <= 0) {
            return -1;
        }
        // A client sending one byte at a time must not extend the deadline indefinitely.
        socketWrapper.updateLastRead();
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
                    return respond("308 Permanent Redirect", origin + requestTarget(line, parser));
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
        sc.write(response);
        if (response.hasRemaining()) {
            return SelectionKey.OP_WRITE;
        }
        // A full close with unread input can reset the connection and discard the redirect at the client.
        sc.shutdownOutput();
        state = State.DRAIN;
        drainStarted = System.nanoTime();
        return drainRequest();
    }

    private int drainRequest() throws IOException {
        // Bound the total drain time, including clients that continuously send data.
        long remaining = Math.min(timeoutMillis, 5000)
            - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - drainStarted);
        if (remaining <= 0) {
            return -1;
        }
        socketWrapper.updateLastRead();
        socketWrapper.setReadTimeout(remaining);
        netInBuffer.clear();
        return sc.read(netInBuffer) == -1 ? -1 : SelectionKey.OP_READ;
    }

    static String requestTarget(String line) {
        return requestTarget(line, new HttpParser(null, null));
    }

    static String requestTarget(String line, HttpParser parser) {
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
        if (target.isEmpty() || target.chars().anyMatch(c -> c <= 32 || c >= 127 || c == '#')) {
            throw new IllegalArgumentException("Invalid HTTP request target");
        }
        if ("*".equals(target) && "OPTIONS".equals(parts[0])) {
            return "/";
        }
        if (!target.startsWith("/")) {
            int schemeEnd = target.indexOf("://");
            if (schemeEnd < 0) {
                throw new IllegalArgumentException("Invalid absolute HTTP request target");
            }
            int authorityEnd = target.length();
            for (char delimiter : new char[]{'/', '?'}) {
                int index = target.indexOf(delimiter, schemeEnd + 3);
                if (index >= 0) {
                    authorityEnd = Math.min(authorityEnd, index);
                }
            }
            URI authority = URI.create(target.substring(0, authorityEnd));
            if ((!"http".equalsIgnoreCase(authority.getScheme()) && !"https".equalsIgnoreCase(authority.getScheme()))
                || authority.getHost() == null || authority.getRawUserInfo() != null) {
                throw new IllegalArgumentException("Invalid absolute HTTP request target");
            }
            target = target.substring(authorityEnd);
            if (!target.startsWith("/")) {
                target = "/" + target;
            }
        }
        StringBuilder encoded = new StringBuilder(target.length());
        boolean query = false;
        for (int i = 0; i < target.length(); i++) {
            char c = target.charAt(i);
            if (c == '?') {
                query = true;
            }
            if (!(query ? parser.isQueryRelaxed(c) : parser.isAbsolutePathRelaxed(c))) {
                throw new IllegalArgumentException("Invalid HTTP request target");
            }
            if (c == '%' && (i + 2 >= target.length()
                || Character.digit(target.charAt(i + 1), 16) < 0 || Character.digit(target.charAt(i + 2), 16) < 0)) {
                throw new IllegalArgumentException("Invalid percent encoding in HTTP request target");
            }
            // Relaxed connector characters must be encoded to form a usable Location URI.
            if ("\"<>[\\]^`{|}".indexOf(c) >= 0) {
                encoded.append('%').append("0123456789ABCDEF".charAt(c >> 4)).append("0123456789ABCDEF".charAt(c & 15));
            } else {
                encoded.append(c);
            }
        }
        // Concatenation (not URI.resolve) keeps // paths from replacing the configured authority.
        return encoded.toString();
    }
}
