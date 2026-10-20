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

import org.apache.tomcat.util.http.parser.HttpParser;
import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.net.SocketBufferHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpsRedirectChannelTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "GET /api/a%2Fb?x=%252F&y=%0d%0a HTTP/1.1|/api/a%2Fb?x=%252F&y=%0d%0a",
        "POST /login HTTP/1.0|/login",
        "DELETE /api HTTP/1.1|/api",
        "OPTIONS * HTTP/1.1|/",
        "GET http://untrusted.example/a%2Fb?q=1 HTTP/1.1|/a%2Fb?q=1",
        "GET http://untrusted.example?x=1 HTTP/1.1|/?x=1",
        "GET //untrusted.example/a HTTP/1.1|//untrusted.example/a"
    })
    void preservesRawTargetWithoutUsingClientAuthority(String line, String target) {
        assertEquals(target, HttpsRedirectChannel.requestTarget(line + "\r\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "GET /a#fragment HTTP/1.1", "GET /a\\b HTTP/1.1", "GET /%zz HTTP/1.1",
        "CONNECT host:443 HTTP/1.1", "GET * HTTP/1.1", "GET / HTTP/2.0",
        "GET /a\tb HTTP/1.1", "GET /a\rLocation:https://evil HTTP/1.1",
        "GET https://user@host/path HTTP/1.1", "GET ftp://host/path HTTP/1.1",
        "GET /a b HTTP/1.1", "GET /é HTTP/1.1", "GET  HTTP/1.1"
    })
    void rejectsInvalidTargets(String line) {
        assertThrows(IllegalArgumentException.class, () -> HttpsRedirectChannel.requestTarget(line + "\r\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"|", "{", "}", "^", "[", "]", "\\", "\"", "<", ">", "`"})
    void acceptsAndEncodesOnlyConfiguredRelaxedCharacters(String character) {
        String escaped = "%" + Integer.toHexString(character.charAt(0)).toUpperCase(java.util.Locale.ROOT);
        String pathLine = "GET /a" + character + "b?q=%2F HTTP/1.1\r\n";
        String queryLine = "GET /x?filter=a" + character + "b HTTP/1.1\r\n";
        assertThrows(IllegalArgumentException.class, () -> HttpsRedirectChannel.requestTarget(pathLine));
        assertThrows(IllegalArgumentException.class, () -> HttpsRedirectChannel.requestTarget(queryLine));
        assertEquals("/a" + escaped + "b?q=%2F",
            HttpsRedirectChannel.requestTarget(pathLine, new HttpParser(character, null)));
        assertEquals("/x?filter=a" + escaped + "b",
            HttpsRedirectChannel.requestTarget(queryLine, new HttpParser(null, character)));
        assertThrows(IllegalArgumentException.class,
            () -> HttpsRedirectChannel.requestTarget(pathLine, new HttpParser(null, character)));
    }

    @Test
    void supportsRelaxedAbsoluteTargetsWithoutTrustingTheirAuthority() {
        assertEquals("/x?filter=a%7Cb", HttpsRedirectChannel.requestTarget(
            "GET http://[::1]:1234/x?filter=a|b HTTP/1.1\r\n", new HttpParser(null, "|")));
    }

    @Test
    void finishesPartialWritesBeforeClosing() throws Exception {
        var socket = mock(SocketChannel.class);
        var output = new ByteArrayOutputStream();
        byte[] request = "GET /partial HTTP/1.1\r\n".getBytes(StandardCharsets.US_ASCII);
        when(socket.read(any(ByteBuffer.class))).thenAnswer(call -> {
            call.<ByteBuffer>getArgument(0).put(request);
            return request.length;
        });
        when(socket.write(any(ByteBuffer.class))).thenAnswer(call -> {
            var buffer = call.<ByteBuffer>getArgument(0);
            int count = Math.min(7, buffer.remaining());
            for (int i = 0; i < count; i++) {
                output.write(buffer.get());
            }
            return count;
        });
        var channel = new HttpsRedirectChannel(new SocketBufferHandler(1024, 1024, false),
            new NioEndpoint(), "https://example.com", 1024, new HttpParser(null, null));
        channel.reset(socket, mock(NioEndpoint.NioSocketWrapper.class));
        assertEquals(SelectionKey.OP_WRITE, channel.handshake(true, false));
        int result = SelectionKey.OP_WRITE;
        for (int i = 0; i < 100 && result == SelectionKey.OP_WRITE; i++) {
            result = channel.handshake(false, true);
        }
        assertEquals(SelectionKey.OP_READ, result);
        verify(socket).shutdownOutput();
        when(socket.read(any(ByteBuffer.class))).thenReturn(-1);
        assertEquals(-1, channel.handshake(true, false));
        assertEquals("HTTP/1.1 308 Permanent Redirect\r\nLocation: https://example.com/partial\r\n"
            + "Content-Length: 0\r\nConnection: close\r\n\r\n", output.toString(StandardCharsets.US_ASCII));
        assertFalse(channel.isHandshakeComplete());
    }
}
