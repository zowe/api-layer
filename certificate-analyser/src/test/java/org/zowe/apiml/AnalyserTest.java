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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyserTest {
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
    void providedCorrectInputs_certificateIsVerified() {
        String[] args = {"--keystore", "../keystore/localhost/localhost.keystore.p12",
            "--truststore", "../keystore/localhost/localhost.truststore.p12",
            "--keypasswd", "password",
            "--keyalias", "localhost",
            "-l"};
        Analyser.main(args);
        assertTrue(outputStream.toString().contains("Trusted certificate is stored under alias:"));
    }
}
