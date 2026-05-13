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
        System.out.println("DEBUG [ZosmfJwtCheck] Java home:    " + System.getProperty("java.home"));
        System.out.println("DEBUG [ZosmfJwtCheck] OS name:      " + System.getProperty("os.name"));
        System.out.println("DEBUG [ZosmfJwtCheck] java.class.path: " + System.getProperty("java.class.path", "<not set>"));
        System.out.println("DEBUG [ZosmfJwtCheck] java.ext.dirs:   " + System.getProperty("java.ext.dirs", "<not set>"));
        System.out.println("DEBUG [ZosmfJwtCheck] Classloader chain: app=" + ZosmfJwtCheck.class.getClassLoader()
            + " parent=" + (ZosmfJwtCheck.class.getClassLoader() != null ? ZosmfJwtCheck.class.getClassLoader().getParent() : "null")
            + " threadCtx=" + Thread.currentThread().getContextClassLoader());
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
     * Registers the IBM SAF keyring URL protocol handler so that
     * {@code new URL("safkeyring://...")} works on z/OS without requiring the
     * caller to pass {@code -Djava.protocol.handler.pkgs=com.ibm.crypto.provider}.
     * On non-z/OS platforms the handler class is simply not found and is ignored.
     */
    static void ensureSafkeyringHandler() {
        String existing = System.getProperty("java.protocol.handler.pkgs", "");
        System.out.println("DEBUG [ZosmfJwtCheck] ensureSafkeyringHandler: current value='" + existing + "'");
        if (!existing.contains("com.ibm.crypto.provider")) {
            String newValue = existing.isEmpty() ? "com.ibm.crypto.provider" : existing + "|com.ibm.crypto.provider";
            System.setProperty("java.protocol.handler.pkgs", newValue);
            System.out.println("DEBUG [ZosmfJwtCheck] ensureSafkeyringHandler: SET to '" + newValue + "'");
        } else {
            System.out.println("DEBUG [ZosmfJwtCheck] ensureSafkeyringHandler: already contains com.ibm.crypto.provider, no change");
        }

        // TODO: REMOVE diagnostic checks after SAF keyring issue is resolved
        // Check which IBM z/OS keyring-related classes are available on the classpath
        String[] classesToCheck = {
            "com.ibm.crypto.provider.safkeyring.Handler",
            "com.ibm.crypto.provider.safkeyringjce.Handler",
            "com.ibm.crypto.provider.safkeyringjcecca.Handler",
            "com.ibm.crypto.provider.safkeyringjceccaracfks.Handler",
            "com.ibm.crypto.provider.safkeyringjcehybrid.Handler",
            "com.ibm.crypto.zsecurity.provider.RACFInputStream",
            "com.ibm.jsse2.IBMJSSEProvider2",
        };
        for (String className : classesToCheck) {
            try {
                Class<?> cls = Class.forName(className);
                System.out.println("DEBUG [ZosmfJwtCheck] Class FOUND: " + className
                    + " (classLoader=" + cls.getClassLoader() + ")");
            } catch (ClassNotFoundException e) {
                System.out.println("DEBUG [ZosmfJwtCheck] Class NOT FOUND: " + className);
            }
        }

        // List installed security providers
        System.out.println("DEBUG [ZosmfJwtCheck] Installed security providers:");
        for (java.security.Provider p : java.security.Security.getProviders()) {
            System.out.println("DEBUG [ZosmfJwtCheck]   " + p.getName() + " v" + p.getVersionStr()
                + " (" + p.getClass().getName() + ")");
        }

        // Test URL creation inline to capture exact failure point
        System.out.println("DEBUG [ZosmfJwtCheck] Testing URL creation for 'safkeyring://test/test'...");
        try {
            java.net.URL testUrl = new java.net.URL("safkeyring://test/test");
            System.out.println("DEBUG [ZosmfJwtCheck] URL creation SUCCEEDED: " + testUrl);
        } catch (java.net.MalformedURLException e) {
            System.out.println("DEBUG [ZosmfJwtCheck] URL creation FAILED: " + e.getMessage());
            // If the handler class was FOUND above but URL creation FAILED, that confirms
            // the bootstrap classloader theory: Class.forName from our code can find it,
            // but java.net.URL (loaded by bootstrap classloader) cannot.
            System.out.println("DEBUG [ZosmfJwtCheck] URL class classLoader: " + java.net.URL.class.getClassLoader());
            System.out.println("DEBUG [ZosmfJwtCheck] This class classLoader: " + ZosmfJwtCheck.class.getClassLoader());
            System.out.println("DEBUG [ZosmfJwtCheck] If handler class was FOUND above but URL creation FAILED,");
            System.out.println("DEBUG [ZosmfJwtCheck]   this indicates a classloader visibility issue.");
            System.out.println("DEBUG [ZosmfJwtCheck]   The RACFInputStream fallback in openKeyringStream() will be used instead.");
        }
    }

    public static void main(String[] args) {
        System.exit(mainWithExitCode(args));
    }
}
