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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class RemoteHandshakeTest {

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
    void providedCorrectKey_thenRequestPasses() throws Exception {
        String[] args = {"--keystore", "../keystore/localhost/localhost.keystore.p12",
            "--truststore", "../keystore/localhost/localhost.truststore.p12",
            "--keypasswd", "password",
            "--keyalias", "localhost",
            "-r", "https://localhost:10010"};

        ApimlConf conf = new ApimlConf();
        CommandLine.ParseResult cmd = new CommandLine(conf).parseArgs(args);
        Stores stores = new Stores(conf);
        VerifierSSLContext verifierSslContext = VerifierSSLContext.initSSLContext(stores);
        HttpClient client = mock(HttpClient.class);
        RemoteHandshake remoteHandshake = new RemoteHandshake(verifierSslContext, client);
        when(client.executeCall(any())).thenReturn(200);
        remoteHandshake.verify();
        String expectedMsg = "Start of the remote SSL handshake.\n" +
            "Handshake was successful. Service \"https://localhost:10010\" is trusted by truststore \"../keystore/localhost/localhost.keystore.p12\".\n";
        assertEquals(expectedMsg, outputStream.toString());
    }
}
