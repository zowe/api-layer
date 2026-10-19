/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Allows tests to stop in the middle of a frame, which Jakarta's send API cannot express. */
public final class RawWebSocketClient implements AutoCloseable {
    private final Socket socket;
    private final DataOutputStream output;
    private long offset;

    public RawWebSocketClient(SSLContext ssl, int port, String path) throws IOException {
        socket = ssl.getSocketFactory().createSocket("localhost", port);
        socket.setSoTimeout(10_000);
        output = new DataOutputStream(socket.getOutputStream());
        output.write(("GET " + path + " HTTP/1.1\r\nHost: localhost:" + port
            + "\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Version: 13"
            + "\r\nSec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        output.flush();
        var response = new ByteArrayOutputStream();
        while (!response.toString(StandardCharsets.US_ASCII).endsWith("\r\n\r\n")) {
            int value = socket.getInputStream().read();
            if (value < 0 || response.size() > 16384) throw new IOException("Incomplete upgrade response");
            response.write(value);
        }
        if (!response.toString(StandardCharsets.US_ASCII).startsWith("HTTP/1.1 101")) {
            throw new IOException("WebSocket upgrade failed: " + response.toString(StandardCharsets.US_ASCII));
        }
    }

    public void header(int opcode, boolean last, int length) throws IOException {
        output.writeByte(opcode | (last ? 128 : 0));
        if (length < 126) {
            output.writeByte(128 | length);
        } else if (length <= 65535) {
            output.writeByte(128 | 126);
            output.writeShort(length);
        } else {
            output.writeByte(128 | 127);
            output.writeLong(length);
        }
        output.write(new byte[] {1, 2, 3, 4});
        offset = 0;
    }

    public void payload(byte[] bytes) throws IOException {
        var masked = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) masked[i] = (byte) (bytes[i] ^ ((offset++ % 4) + 1));
        output.write(masked);
        output.flush();
    }

    public void frame(int opcode, boolean last, byte[] bytes) throws IOException {
        header(opcode, last, bytes.length);
        payload(bytes);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
