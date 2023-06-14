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

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("squid:S106") //ignoring the System.out System.err warinings
public class Analyser {

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
            SSLContextFactory sslContextFactory = SSLContextFactory.initSSLContextWithoutKeystore(stores);
            List<Verifier> verifiers = new ArrayList<>();
            HttpClient client;
            if (conf.getRemoteUrl() != null) {
                if (conf.isClientCertAuth()) {
                    sslContextFactory = SSLContextFactory.initSSLContextWithKeystore(stores);
                    client = new HttpClient(sslContextFactory.getSslContextWithKeystore());
                } else {
                    client = new HttpClient(sslContextFactory.getSslContext());
                }
                verifiers.add(new RemoteHandshake(sslContextFactory, client));
            } else {
                System.out.println("No remote will be verified. Specify \"-r\" or \"--remoteurl\" if you wish to verify the trust.");
            }

            if (conf.isDoLocalHandshake()) {
                sslContextFactory = SSLContextFactory.initSSLContextWithKeystore(stores);
                client = new HttpClient(sslContextFactory.getSslContextWithKeystore());
                verifiers.add(new LocalHandshake(sslContextFactory, client));
            }
            if (conf.getKeyStore() != null) {
                verifiers.add(new LocalVerifier(stores));
            }
            verifiers.forEach(Verifier::verify);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }


}
