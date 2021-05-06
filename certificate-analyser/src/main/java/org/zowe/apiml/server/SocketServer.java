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
    }

    @Override
    public void run() {
        try (Socket socket = serverSocket.accept()) {
//            newListener();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            String path = getPath(reader);
//            System.out.println(path);
            OutputStream outStream = socket.getOutputStream();
            PrintWriter out = new PrintWriter(outStream);
            out.print("HTTP/1.0 200 OK\r\n");
//            out.print("Content-Length: " + path.getBytes().length +
//                "\r\n");
//            out.print("Content-Type: text/html\r\n\r\n");
            out.flush();
//            outStream.write(path.getBytes());
            outStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void newListener() {
        (new Thread(this)).start();
    }

    private static String getPath(BufferedReader in)
        throws IOException {
        String line = in.readLine();
        String path = "";
        // extract class from GET line
        if (line.startsWith("GET /")) {
            line = line.substring(5, line.length() - 1).trim();
            int index = line.indexOf(' ');
            if (index != -1) {
                path = line.substring(0, index);
            }
        }

        // eat the rest of header
        do {
            line = in.readLine();
        } while ((line.length() != 0) &&
            (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));

        return path;
    }

}
