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
@SuppressWarnings("squid:S106") //ignoring the System.out System.err warinings
public class LocalHandshake implements Verifier {

    private VerifierSSLContext verifierSslContext;
    private HttpClient client;

    public LocalHandshake(VerifierSSLContext verifierSslContext, HttpClient client) {
        this.verifierSslContext = verifierSslContext;
        this.client = client;
    }

    @Override
    public void verify() {
        try { //NOSONAR
            SSLServerSocket listener = (SSLServerSocket) verifierSslContext.getSslContext().getServerSocketFactory().createServerSocket(0);
//            start listening on socket to do a SSL handshake
            new SocketServer(listener);
            String address = "https://localhost:" + listener.getLocalPort();
            String keyAlias = verifierSslContext.getStores().getConf().getKeyAlias();
            String trustStore = verifierSslContext.getStores().getConf().getTrustStore();
            try { //NOSONAR
                System.out.println("Start of the local SSL handshake.");
                client.executeCall(new URL(address));
                System.out.println("Handshake was successful. Certificate stored under alias \"" + keyAlias + "\" is trusted by truststore \"" + trustStore
                    + "\".");
            } catch (SSLHandshakeException e) {
                System.out.println("Handshake failed. Certificate stored under alias \"" + keyAlias + "\" is not trusted by truststore \"" + trustStore
                    + "\". Error message: " + e.getMessage());
            }
        } catch (IOException e) {
            System.out.println("Failed when calling local server. Error message: " + e.getMessage());
        }
    }
}
