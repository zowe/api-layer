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
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * Orchestrates the z/OSMF JWT endpoint check when invoked via the
 * {@code --zosmf-jwt-check} flag from the certificate-analyser JAR.
 *
 * <p>This runner uses the unique classes from the zosmf-jwt-check module
 * ({@link ZosmfJwtCheckConf}, {@link HttpClientWrapper}, {@link JwkEndpointChecker})
 * and creates SSL contexts directly via standard JDK APIs.</p>
 *
 * <p>Exit codes: 0 = success, 4 = failure/error, 8 = help displayed.</p>
 */
@SuppressWarnings("squid:S106")
public class ZosmfJwtCheckRunner {

    static final String VERIFY_STRICT = "STRICT";
    static final String VERIFY_NONSTRICT = "NONSTRICT";
    static final String VERIFY_DISABLED = "DISABLED";

    public static int run(String[] args) {
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

            HttpClientWrapper httpClient = buildHttpClient(conf);

            JwkEndpointChecker checker = new JwkEndpointChecker(httpClient, conf);
            boolean success = checker.check();
            return success ? 0 : 4;
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return 4;
        }
    }

    static HttpClientWrapper buildHttpClient(ZosmfJwtCheckConf conf) throws Exception {
        if (!"https".equalsIgnoreCase(conf.getScheme())) {
            return new HttpClientWrapper();
        }

        String verifyMode = conf.getVerifyCertificates().toUpperCase();

        if (VERIFY_DISABLED.equals(verifyMode)) {
            SSLContext sslContext = createTrustAllSSLContext();
            HostnameVerifier noopVerifier = (hostname, session) -> true;
            return new HttpClientWrapper(sslContext, noopVerifier);
        }

        SSLContext sslContext = createSSLContext(conf);
        HostnameVerifier hostnameVerifier = null;
        if (VERIFY_NONSTRICT.equals(verifyMode)) {
            hostnameVerifier = (hostname, session) -> true;
            System.out.println("INFO: Hostname verification is disabled (NONSTRICT mode).");
        }
        return new HttpClientWrapper(sslContext, hostnameVerifier);
    }

    static SSLContext createTrustAllSSLContext() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    // trust all
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    // trust all
                }
            }
        };

        KeyStore emptyKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
        emptyKeystore.load(null, null);
        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyFactory.init(emptyKeystore, null);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyFactory.getKeyManagers(), trustAllCerts, new SecureRandom());
        System.out.println("WARNING: SSL certificate verification is DISABLED. All certificates will be trusted.");
        return sslContext;
    }

    static SSLContext createSSLContext(ZosmfJwtCheckConf conf) throws Exception {
        // Load truststore
        KeyStore trustStore = loadStore(conf.getTrustStore(), conf.getTrustStorePassword(), conf.getTrustStoreType());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // Load keystore if specified, otherwise use empty
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        if (conf.getKeyStore() != null) {
            KeyStore keyStore = loadStore(conf.getKeyStore(), conf.getKeyStorePassword(), conf.getKeyStoreType());
            kmf.init(keyStore, conf.getKeyStorePassword().toCharArray());
        } else {
            KeyStore emptyKs = KeyStore.getInstance(KeyStore.getDefaultType());
            emptyKs.load(null, null);
            kmf.init(emptyKs, null);
        }

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    static KeyStore loadStore(String path, String password, String type) throws Exception {
        if (Stores.isKeyring(path)) {
            try (InputStream is = Stores.keyRingUrl(path).openStream()) {
                return Stores.readKeyStore(is, password.toCharArray(), type);
            }
        }
        try (InputStream is = new FileInputStream(path)) {
            return Stores.readKeyStore(is, password.toCharArray(), type);
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
}
