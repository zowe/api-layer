/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.server;

import javax.net.ssl.SSLServerSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketServer implements Runnable {

    private SSLServerSocket serverSocket;

    public SocketServer(SSLServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        newListener();
        System.out.println("Listening on port: " + serverSocket.getLocalPort());
    }

    @Override
    public void run() {
        try (Socket socket = serverSocket.accept()) {
            OutputStream outStream = socket.getOutputStream();
            PrintWriter out = new PrintWriter(outStream);
            out.print("HTTP/1.0 200 OK\r\n");
            out.flush();
            outStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void newListener() {
        (new Thread(this)).start();
    }



}
