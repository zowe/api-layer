/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.ws;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.tyrus.server.Server;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.nio.ByteBuffer;

@Component
public class TyrusServer implements InitializingBean {

    @Value("")
    private int port;

    @Value("")
    private String hostname;

    private Server server;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.server = new Server(StringUtils.isBlank(hostname) ? "localhost" : hostname, port > 1024 ? port : 0, "/", null, BinaryEchoServer.class);
        this.server.start();
    }

    @ServerEndpoint("/echo")
    @Slf4j
    public static class BinaryEchoServer {

        @OnMessage
        public void onBinaryMessage(ByteBuffer message, Session session) {
            log.info("Got binary message {} bytes", message.remaining());
            try {
                sendBinaryChunked(session, message, 8192);
            } catch (IOException e) {
                log.error("Error sending back binary chunk: {}", e.getMessage(), e);
            }
        }

        public static void sendBinaryChunked(Session session, ByteBuffer buffer, int chunkSize) throws IOException {
            ByteBuffer src = buffer.duplicate();
            while (src.hasRemaining()) {
                int len = Math.min(chunkSize, src.remaining());
                ByteBuffer chunk = src.slice();
                chunk.limit(len);
                src.position(src.position() + len);
                session.getBasicRemote().sendBinary(chunk, !src.hasRemaining());
            }

        }

    }

}
