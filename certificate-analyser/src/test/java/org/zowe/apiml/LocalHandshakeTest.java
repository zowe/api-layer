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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import static org.junit.jupiter.api.Assertions.*;

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
    void providedCorrectInputs_thenSuccessMessageIsDisplayed() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String[] args = {"--keystore", "../keystore/localhost/localhost.keystore.p12",
            "--truststore", "../keystore/localhost/localhost.keystore.p12",
            "--keypasswd", "password",
            "--keyalias", "localhost",
            "-l"};
        ApimlConf conf = new ApimlConf();
        CommandLine cmdLine = new CommandLine(conf);
        cmdLine.parseArgs(args);
        Stores stores = new Stores(conf);
        VerifierSSLContext sslContext = VerifierSSLContext.initSSLContext(stores);
        HttpClient client = new HttpClient(sslContext.getSslContext());
        Verifier localHandshake = new LocalHandshake(sslContext,client);
        localHandshake.verify();
        assertTrue(outputStream.toString().contains("Handshake was successful. Certificate stored under alias \"" + conf.getKeyAlias() + "\" is trusted by truststore \"" + conf.getTrustStore()
            + "\"."));
    }
}
