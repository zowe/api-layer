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

import org.zowe.apiml.zosmf.jwt.check.ZosmfJwtCheck;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("squid:S106") //ignoring the System.out System.err warnings
public class Analyser {

    private static final String ZOSMF_JWT_CHECK_FLAG = "--zosmf-jwt-check";

    public static int mainWithExitCode(String[] args) {
        org.zowe.apiml.common.KeyringUtils.ensureSafkeyringHandler();

        if (args.length > 0 && ZOSMF_JWT_CHECK_FLAG.equals(args[0])) {
            String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);
            return ZosmfJwtCheck.mainWithExitCode(remainingArgs);
        }

        try {
            ApimlConf conf = new ApimlConf();
            CommandLine cmd = new CommandLine(conf);
            cmd.parseArgs(args);
            if (conf.isHelpRequested()) {
                cmd.printVersionHelp(System.out);
                CommandLine.usage(new ApimlConf(), System.out);
                return 8;
            }

            Stores stores = new Stores(conf);
            SSLContextHolder sslContextHolder = SSLContextHolder.initSSLContextWithoutKeystore(stores);
            List<Verifier> verifiers = new ArrayList<>();
            HttpClient client;
            if (conf.getRemoteUrl() != null) {
                if (conf.isClientCertAuth()) {
                    sslContextHolder = SSLContextHolder.initSSLContextWithKeystore(stores);
                    client = new HttpClient(sslContextHolder.getSslContextWithKeystore());
                } else {
                    client = new HttpClient(sslContextHolder.getSslContext());
                }
                verifiers.add(new RemoteHandshake(sslContextHolder, client));
            } else {
                System.out.println("No remote will be verified. Specify \"-r\" or \"--remoteurl\" if you wish to verify the trust.");
            }

            if (conf.isDoLocalHandshake()) {
                sslContextHolder = SSLContextHolder.initSSLContextWithKeystore(stores);
                client = new HttpClient(sslContextHolder.getSslContextWithKeystore());
                verifiers.add(new LocalHandshake(sslContextHolder, client));
            }
            if (conf.getKeyStore() != null) {
                verifiers.add(new LocalVerifier(stores, conf.getRequiredHostNames()));
            }

            boolean valid = verifiers.stream().map(Verifier::verify).min(Boolean::compareTo).orElse(false);
            return valid ? 0 : 4;
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return 4;
    }

    public static final void main(String[] args) {
        System.exit(mainWithExitCode(args));
    }

}
