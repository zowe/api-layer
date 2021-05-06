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

public class Main {

    public static void main(String[] args) {
        try {
            ApimlConf conf = new ApimlConf();
            CommandLine cmd = new CommandLine(conf);
            cmd.parseArgs(args);
            if (conf.isHelpRequested()) {
                cmd.printVersionHelp(System.out);
                CommandLine.usage(new ApimlConf(), System.out);
                return;
            }
            Stores stores = new Stores(conf);
            VerifierSSLContext verifierSslContext = VerifierSSLContext.initSSLContext(stores);

            Verifier verifier;
            if (conf.getRemoteUrl() != null) {
                verifier = new RemoteHandshakeVerifier(verifierSslContext);
                verifier.verify();

            } else {
                System.out.println("No remote will be verified. Specify \"-r\" or \"--remoteurl\" if you wish to verify the trust.");
            }

            if (conf.isDoLocalHandshake()) {
                verifier = new LocalHandshakeVerifier(verifierSslContext);
                verifier.verify();
            }

            Verifier localVerifier = new LocalVerifier(stores);
            localVerifier.verify();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
