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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

@SuppressWarnings("squid:S106") //ignoring the System.out System.err warinings
public class SocketServer implements Runnable {

    private SSLServerSocket serverSocket;

    public SocketServer(SSLServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        System.out.println("Opening socket.");
        newListener();
        System.out.println("Listening on port: " + serverSocket.getLocalPort());
    }

    @Override
    public void run() {
        try (Socket socket = serverSocket.accept()) {
            newListener();
            OutputStream outStream = socket.getOutputStream();
            PrintWriter out = new PrintWriter(outStream);
            out.print("HTTP/1.0 200 OK\r\n");
            out.flush();
            outStream.flush();
            Thread.sleep(10); //primitive server closes too fast, client fails to read response, so it has to wait.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }

    private void newListener() {
        (new Thread(this)).start();
    }


}
