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

import org.apache.commons.cli.DefaultParser;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import picocli.CommandLine;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class RemoteCertCheckerTest {



    @Test
    void providedCorrectKey_thenRequestPasses() throws Exception{
        String[] args = {"--keystore", "../keystore/localhost/localhost.keystore.p12",
            "--truststore","../keystore/localhost/localhost.keystore.p12",
            "--keypasswd","password",
            "--keyalias","localhost"};
        CLISetup cli = new CLISetup(new DefaultParser(),args);

        ApimlConf conf = new ApimlConf();
        CommandLine.ParseResult cmd = new CommandLine(conf).parseArgs(args);
        Stores stores = new Stores(conf);
        RemoteVerifier remoteVerifier = new RemoteVerifier();
        HttpsURLConnection conn = mock(HttpsURLConnection.class);
        URL url = mock(URL.class);
        when(url.openConnection()).thenReturn(conn);
        when(conn.getResponseCode()).thenReturn(200);
        assertEquals(200,remoteVerifier.verifyEndpoint(stores,url));
    }
}
