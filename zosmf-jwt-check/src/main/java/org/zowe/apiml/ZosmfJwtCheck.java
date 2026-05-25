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
// TODO: REMOVE all "DEBUG [ZosmfJwtCheck]" logging lines after SAF keyring issue is resolved
@SuppressWarnings("squid:S106")
public class ZosmfJwtCheck {

    static final String VERIFY_STRICT = "STRICT";
    static final String VERIFY_NONSTRICT = "NONSTRICT";
    static final String VERIFY_DISABLED = "DISABLED";

    public static int mainWithExitCode(String[] args) {
        System.out.println("DEBUG [ZosmfJwtCheck] ===== zosmf-jwt-check starting =====");
        System.out.println("DEBUG [ZosmfJwtCheck] Java version: " + System.getProperty("java.version"));
        System.out.println("DEBUG [ZosmfJwtCheck] Java vendor:  " + System.getProperty("java.vendor"));
        System.out.println("DEBUG [ZosmfJwtCheck] OS name:      " + System.getProperty("os.name"));
        System.out.println("DEBUG [ZosmfJwtCheck] java.protocol.handler.pkgs (BEFORE ensureSafkeyringHandler): "
            + System.getProperty("java.protocol.handler.pkgs", "<not set>"));

        ensureSafkeyringHandler();

        System.out.println("DEBUG [ZosmfJwtCheck] java.protocol.handler.pkgs (AFTER  ensureSafkeyringHandler): "
            + System.getProperty("java.protocol.handler.pkgs", "<not set>"));

        try {
            ZosmfJwtCheckConf conf = new ZosmfJwtCheckConf();
            CommandLine cmd = new CommandLine(conf);
            cmd.parseArgs(args);

            if (conf.isHelpRequested()) {
                cmd.printVersionHelp(System.out);
                CommandLine.usage(new ZosmfJwtCheckConf(), System.out);
                return 8;
            }

            System.out.println("DEBUG [ZosmfJwtCheck] Parsed config:");
            System.out.println("DEBUG [ZosmfJwtCheck]   zosmf-host=" + conf.getZosmfHost());
            System.out.println("DEBUG [ZosmfJwtCheck]   zosmf-port=" + conf.getZosmfPort());
            System.out.println("DEBUG [ZosmfJwtCheck]   scheme=" + conf.getScheme());
            System.out.println("DEBUG [ZosmfJwtCheck]   verify-certificates=" + conf.getVerifyCertificates());
            System.out.println("DEBUG [ZosmfJwtCheck]   truststore-file=" + conf.getTrustStore());
            System.out.println("DEBUG [ZosmfJwtCheck]   truststore-type=" + conf.getTrustStoreType());
            System.out.println("DEBUG [ZosmfJwtCheck]   truststore-isKeyring=" + Stores.isKeyring(conf.getTrustStore()));
            System.out.println("DEBUG [ZosmfJwtCheck]   keystore-file=" + conf.getKeyStore());
            System.out.println("DEBUG [ZosmfJwtCheck]   keystore-type=" + conf.getKeyStoreType());

            validateConfig(conf);

            HttpClientWrapper httpClient;
            if ("https".equalsIgnoreCase(conf.getScheme())) {
                String verifyMode = conf.getVerifyCertificates().toUpperCase();

                if (VERIFY_DISABLED.equals(verifyMode)) {
                    SSLContextFactory sslContextFactory = SSLContextFactory.initTrustAllSSLContext();
                    HostnameVerifier noopVerifier = (hostname, session) -> true;
                    httpClient = new HttpClientWrapper(sslContextFactory.getSslContext(), noopVerifier);
                } else {
                    System.out.println("DEBUG [ZosmfJwtCheck] Creating Stores (truststore/keystore)...");
                    Stores stores = new Stores(conf);
                    System.out.println("DEBUG [ZosmfJwtCheck] Stores created successfully. Building SSLContext...");
                    SSLContextFactory sslContextFactory = SSLContextFactory.initSSLContext(stores);
                    System.out.println("DEBUG [ZosmfJwtCheck] SSLContext created successfully.");

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
            System.out.println("DEBUG [ZosmfJwtCheck] Calling JwkEndpointChecker.check()...");
            boolean success = checker.check();
            System.out.println("DEBUG [ZosmfJwtCheck] JwkEndpointChecker.check() returned: " + success);
            return success ? 0 : 4;
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.err.println("DEBUG [ZosmfJwtCheck] Exception class: " + e.getClass().getName());
            e.printStackTrace(System.err);
            return 4;
        }
    }

    static void validateConfig(ZosmfJwtCheckConf conf) {
        String scheme = conf.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("--scheme must be 'http' or 'https', got: " + scheme);
        }

        String verifyMode = conf.getVerifyCertificates().toUpperCase();
        if (!VERIFY_STRICT.equals(verifyMode) && !VERIFY_NONSTRICT.equals(verifyMode) && !VERIFY_DISABLED.equals(verifyMode)) {
            throw new IllegalArgumentException("--verify-certificates must be STRICT, NONSTRICT, or DISABLED, got: " + conf.getVerifyCertificates());
        }

        if ("https".equalsIgnoreCase(scheme) && !VERIFY_DISABLED.equals(verifyMode)) {
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
     * Attempts to register the IBM SAF keyring URL protocol handler via the legacy
     * {@code java.protocol.handler.pkgs} system property.
     *
     * <p><b>Note:</b> On IBM Java 17/21 (z/OS), the safkeyring handler is registered
     * via the {@code java.net.spi.URLStreamHandlerProvider} SPI in module
     * {@code ibm.crypto.zsecurity}, so this property has no effect. The real fix is
     * to launch the JVM with {@code --add-modules ibm.crypto.zsecurity}.
     * This method is retained only as a best-effort fallback for IBM Java 8.</p>
     */
    static void ensureSafkeyringHandler() {
        String[] packagePrefixes = {
            "com.ibm.crypto.zsecurity.provider",
            "com.ibm.crypto.hdwrCCA.provider"
        };
        String existing = System.getProperty("java.protocol.handler.pkgs", "");
        System.out.println("DEBUG [ZosmfJwtCheck] ensureSafkeyringHandler: existing java.protocol.handler.pkgs='" + existing + "'");
        StringBuilder sb = new StringBuilder(existing);
        for (String prefix : packagePrefixes) {
            if (!existing.contains(prefix)) {
                if (sb.length() > 0) {
                    sb.append('|');
                }
                sb.append(prefix);
                System.out.println("DEBUG [ZosmfJwtCheck] ensureSafkeyringHandler: Adding package prefix: " + prefix);
            } else {
                System.out.println("DEBUG [ZosmfJwtCheck] ensureSafkeyringHandler: Already present: " + prefix);
            }
        }
        String finalValue = sb.toString();
        System.setProperty("java.protocol.handler.pkgs", finalValue);
        System.out.println("DEBUG [ZosmfJwtCheck] ensureSafkeyringHandler: Final java.protocol.handler.pkgs='" + finalValue + "'");
        System.out.println("DEBUG [ZosmfJwtCheck] ensureSafkeyringHandler: NOTE - On Java 17/21, this property has no effect.");
        System.out.println("DEBUG [ZosmfJwtCheck] ensureSafkeyringHandler: The safkeyring handler is loaded via SPI from module ibm.crypto.zsecurity (requires --add-modules).");
    }

    public static void main(String[] args) {
        System.exit(mainWithExitCode(args));
    }
}
