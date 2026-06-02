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

import javax.net.ssl.HostnameVerifier;

/**
 * Entry point and orchestrator for the z/OSMF JWT check tool.
 * Parses CLI arguments, validates configuration, builds the appropriate
 * SSL context and HTTP client, then delegates to {@link JwkEndpointChecker}.
 *
 * <p>Exit codes: 0 = success, 4 = failure/error, 8 = help displayed.</p>
 */
@SuppressWarnings("squid:S106")
public class ZosmfJwtCheck {

    static final String SCHEME_HTTPS = "https";
    static final String VERIFY_STRICT = "STRICT";
    static final String VERIFY_NONSTRICT = "NONSTRICT";
    static final String VERIFY_DISABLED = "DISABLED";

    public static int mainWithExitCode(String[] args) {
        ensureSafkeyringHandler();

        try {
            ZosmfJwtCheckConf conf = new ZosmfJwtCheckConf();
            CommandLine cmd = new CommandLine(conf);
            cmd.parseArgs(args);

            if (conf.isHelpRequested()) {
                cmd.printVersionHelp(System.out);
                CommandLine.usage(new ZosmfJwtCheckConf(), System.out);
                return 8;
            }

            validateConfig(conf);

            HttpClientWrapper httpClient;
            if (SCHEME_HTTPS.equalsIgnoreCase(conf.getScheme())) {
                String verifyMode = conf.getVerifyCertificates().toUpperCase();

                if (VERIFY_DISABLED.equals(verifyMode)) {
                    SSLContextFactory sslContextFactory = SSLContextFactory.initTrustAllSSLContext();
                    HostnameVerifier noopVerifier = (hostname, session) -> true;
                    httpClient = new HttpClientWrapper(sslContextFactory.getSslContext(), noopVerifier);
                } else {
                    ZosmfStores stores = new ZosmfStores(conf);
                    SSLContextFactory sslContextFactory = SSLContextFactory.initSSLContext(stores);

                    HostnameVerifier hostnameVerifier;
                    if (VERIFY_NONSTRICT.equals(verifyMode)) {
                        hostnameVerifier = (hostname, session) -> true;
                        System.out.println("INFO: Hostname verification is disabled (NONSTRICT mode).");
                    } else {
                        hostnameVerifier = null; // use default JDK hostname verifier
                    }
                    httpClient = new HttpClientWrapper(sslContextFactory.getSslContext(), hostnameVerifier);
                }
            } else {
                httpClient = new HttpClientWrapper();
            }

            JwkEndpointChecker checker = new JwkEndpointChecker(httpClient, conf);
            return checker.check() ? 0 : 4;
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            return 4;
        }
    }

    static void validateConfig(ZosmfJwtCheckConf conf) {
        String scheme = conf.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !SCHEME_HTTPS.equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("--scheme must be 'http' or 'https', got: " + scheme);
        }

        String verifyMode = conf.getVerifyCertificates().toUpperCase();
        if (!VERIFY_STRICT.equals(verifyMode) && !VERIFY_NONSTRICT.equals(verifyMode) && !VERIFY_DISABLED.equals(verifyMode)) {
            throw new IllegalArgumentException("--verify-certificates must be STRICT, NONSTRICT, or DISABLED, got: " + conf.getVerifyCertificates());
        }

        if (SCHEME_HTTPS.equalsIgnoreCase(scheme) && !VERIFY_DISABLED.equals(verifyMode)) {
            if (conf.getTrustStore() == null) {
                throw new IllegalArgumentException("--truststore-file is required when --scheme=https and verification is not DISABLED. " +
                    "Provide the path to the truststore containing the z/OSMF server certificate.");
            }
            if (conf.getTrustStorePassword() == null) {
                throw new IllegalArgumentException("--truststore-password is required when --scheme=https and verification is not DISABLED.");
            }
        }
    }

    /**
     * Registers IBM SAF keyring URL protocol handler packages via the
     * {@code java.protocol.handler.pkgs} system property.
     *
     * <p>On IBM Java 17/21 (z/OS), this property works in conjunction with
     * {@code --add-modules ibm.crypto.zsecurity,ibm.crypto.hdwrcca} to enable
     * the {@code safkeyring://} URL protocol. The {@code --add-modules} flag
     * resolves the module (making classes accessible), while this property tells
     * the {@link java.net.URL} class which packages to search for the handler.</p>
     */
    static void ensureSafkeyringHandler() {
        String[] packagePrefixes = {
            "com.ibm.crypto.zsecurity.provider",
            "com.ibm.crypto.hdwrCCA.provider"
        };
        String existing = System.getProperty("java.protocol.handler.pkgs", "");
        StringBuilder sb = new StringBuilder(existing);
        for (String prefix : packagePrefixes) {
            if (!existing.contains(prefix)) {
                if (!sb.isEmpty()) {
                    sb.append('|');
                }
                sb.append(prefix);
            }
        }
        System.setProperty("java.protocol.handler.pkgs", sb.toString());
    }

    public static void main(String[] args) {
        System.exit(mainWithExitCode(args));
    }
}
