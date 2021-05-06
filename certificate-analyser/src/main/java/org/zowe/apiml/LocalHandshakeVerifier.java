/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml;

import org.zowe.apiml.server.SocketServer;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.net.URL;

public class LocalHandshakeVerifier extends HandshakeVerifier {

    private VerifierSSLContext verifierSslContext;

    public LocalHandshakeVerifier(VerifierSSLContext verifierSslContext) {
        this.verifierSslContext = verifierSslContext;
    }

    @Override
    public VerifierSSLContext getVerifierSslContext() {
        return verifierSslContext;
    }

    @Override
    public void verify() {
        try {
            SSLServerSocket listener = (SSLServerSocket) verifierSslContext.getSslContext().getServerSocketFactory().createServerSocket(0);
            SocketServer server = new SocketServer(listener);
            String address = "https://localhost:" + listener.getLocalPort();
            try {
                int response = executeCall(new URL(address));
                System.out.println("Certificate for " + address + " is trusted. Response code: " + response);
            } catch (SSLHandshakeException e) {
                System.out.println("Certificate at " + address +
                    " is not trusted. Please add CA of this certificate to your truststore." + getVerifierSslContext().getStores().getConf().getTrustStore());
            } catch (IOException e) {
                System.out.println("Error calling endpoint " + e.getMessage());
            }
        }
         catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
