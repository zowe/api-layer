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

import javax.net.ssl.SSLHandshakeException;
import java.net.MalformedURLException;
import java.net.URL;

public class RemoteHandshake implements Verifier {

    private VerifierSSLContext verifierSslContext;
    private HttpClient httpClient;

    public RemoteHandshake(VerifierSSLContext verifierSslContext, HttpClient httpClient) {
        this.verifierSslContext = verifierSslContext;
        this.httpClient = httpClient;
    }

    public void verify() {
        String serviceAddress = verifierSslContext.getStores().getConf().getRemoteUrl();
        String trustStore = verifierSslContext.getStores().getConf().getTrustStore();

        try {
            URL url = new URL(serviceAddress);
            System.out.println("Start of the remote SSL handshake.");
            httpClient.executeCall(url);
            System.out.println("Handshake was successful. Service \"" + serviceAddress + "\" is trusted by truststore \"" + trustStore
                + "\".");
        } catch (MalformedURLException e) {
            System.out.println("Incorrect url \"" + serviceAddress + "\". Error message: " + e.getMessage());
        } catch (SSLHandshakeException e) {
            System.out.println("Handshake failed. Service \"" + serviceAddress +
                "\" is not trusted. Please add CA of this certificate to your truststore " + trustStore);
        } catch (Exception e) {
            System.out.println("Failed when calling url: \"" + serviceAddress + "\" Error message: " + e.getMessage());
        }
    }


}
