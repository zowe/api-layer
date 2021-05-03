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

import picocli.CommandLine;

import java.net.URL;

public class Main {

    public static void main(String[] args) {
        try {
            ApimlConf conf = new ApimlConf();
            CommandLine.ParseResult cmd = new CommandLine(conf).parseArgs(args);
            Stores stores = new Stores(conf);

            if (conf.getRemoteUrl() != null) {
                URL remote = new URL(conf.getRemoteUrl());
                RemoteVerifier remoteVerifier = new RemoteVerifier();
                int returnCode = remoteVerifier.verifyEndpoint(stores, remote);
                System.out.println("API ML CA is in trustStore:" + (returnCode != 0));
            } else {
                System.out.println("No remote will be verified. Specify \"-r\" or \"--remoteurl\" if you wish to verify this certificate");
            }
            LocalVerifier localVerifier = new LocalVerifier();
            boolean trustedCert = localVerifier.verifyLocalKeystore(stores);
            System.out.println("Certificate is trusted by services: " + trustedCert);
            localVerifier.printDetails(stores);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
