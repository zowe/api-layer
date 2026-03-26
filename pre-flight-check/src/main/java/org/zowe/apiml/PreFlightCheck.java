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

@SuppressWarnings("squid:S106")
public class PreFlightCheck {

    public static int mainWithExitCode(String[] args) {
        try {
            PreFlightCheckConf conf = new PreFlightCheckConf();
            CommandLine cmd = new CommandLine(conf);
            cmd.parseArgs(args);

            if (conf.isHelpRequested()) {
                cmd.printVersionHelp(System.out);
                CommandLine.usage(new PreFlightCheckConf(), System.out);
                return 8;
            }

            validateConfig(conf);

            HttpClientWrapper httpClient;
            if ("https".equalsIgnoreCase(conf.getScheme())) {
                Stores stores = new Stores(conf);
                SSLContextFactory sslContextFactory = SSLContextFactory.initSSLContext(stores);
                httpClient = new HttpClientWrapper(sslContextFactory.getSslContext());
            } else {
                httpClient = new HttpClientWrapper();
            }

            JwkEndpointChecker checker = new JwkEndpointChecker(httpClient, conf);
            boolean success = checker.check();
            return success ? 0 : 4;
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return 4;
        }
    }

    static void validateConfig(PreFlightCheckConf conf) {
        String scheme = conf.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("--scheme must be 'http' or 'https', got: " + scheme);
        }

        if ("https".equalsIgnoreCase(scheme)) {
            if (conf.getTrustStore() == null) {
                throw new IllegalArgumentException("--truststore is required when --scheme=https. " +
                    "Provide the path to the truststore containing the z/OSMF server certificate.");
            }
            if (conf.getTrustStorePassword() == null) {
                throw new IllegalArgumentException("--truststore-password is required when --scheme=https.");
            }
        }
    }

    public static void main(String[] args) {
        System.exit(mainWithExitCode(args));
    }
}
