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

import org.junit.jupiter.api.*;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.*;
import java.security.cert.CertificateException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

class LocalHandshakeTest {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setupStreams() {
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void providedCorrectInputs_thenSuccessMessageIsDisplayed() throws IOException, CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String[] args = {"--keystore", "../keystore/localhost/localhost.keystore.p12",
            "--truststore", "../keystore/localhost/localhost.truststore.p12",
            "--keypasswd", "password",
            "--keyalias", "localhost",
            "-l"};
        ApimlConf conf = new ApimlConf();
        CommandLine cmdLine = new CommandLine(conf);
        cmdLine.parseArgs(args);
        Stores stores = new Stores(conf);
        SSLContextFactory sslContext = SSLContextFactory.initSSLContextWithKeystore(stores);
        HttpClient client = new HttpClient(sslContext.getSslContextWithKeystore());
        Verifier localHandshake = new LocalHandshake(sslContext, client);
        localHandshake.verify();
        assertThat(outputStream.toString(), containsString("Handshake was successful. Certificate stored under alias \"" + conf.getKeyAlias() + "\" is trusted by truststore \"" + conf.getTrustStore()
            + "\"."));
    }

    @Test
    void providedNotTrustedKey_thenHandshakeExceptionIsDisplayed() throws IOException, CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String[] args = {"--keystore", "../keystore/selfsigned/localhost.keystore.p12",
            "--truststore", "../keystore/localhost/localhost.truststore.p12",
            "--keypasswd", "password",
            "--keyalias", "localhost",
            "-l"};
        ApimlConf conf = new ApimlConf();
        CommandLine cmdLine = new CommandLine(conf);
        cmdLine.parseArgs(args);
        Stores stores = new Stores(conf);
        SSLContextFactory sslContext = SSLContextFactory.initSSLContextWithKeystore(stores);
        HttpClient client = new HttpClient(sslContext.getSslContextWithKeystore());
        Verifier localHandshake = new LocalHandshake(sslContext, client);
        localHandshake.verify();
        assertThat(outputStream.toString(), containsString("Handshake failed. Certificate stored under alias \"" + conf.getKeyAlias() + "\" is not trusted by truststore"));
    }
}
