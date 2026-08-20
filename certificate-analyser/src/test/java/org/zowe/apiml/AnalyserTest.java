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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyserTest {
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setupStreams() {
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errStream));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }


    @Test
    void providedCorrectInputs_certificateIsVerified() {
        String[] args = {"--keystore", "../keystore/service/service.keystore.p12",
            "--truststore", "../keystore/service/service.truststore.p12",
            "--keypasswd", "password",
            "--keyalias", "localhost",
            "-l"};
        assertEquals(0, Analyser.mainWithExitCode(args));
        assertTrue(outputStream.toString().contains("Trusted certificate is stored under alias:"));
    }

    @Test
    void whenHelpRequested_thenHelpIsPrintedAndExitCodeIs8() {
        String[] args = {"-h"};
        assertEquals(8, Analyser.mainWithExitCode(args));
        assertTrue(outputStream.toString().contains("Usage:"));
        assertTrue(outputStream.toString().contains("Display a help message"));
    }

    @Test
    void whenZosmfJwtCheckFlagPassed_thenDelegatesToZosmfJwtCheck() {
        String[] args = {"--zosmf-jwt-check", "--help"};
        assertEquals(8, Analyser.mainWithExitCode(args));
        assertTrue(outputStream.toString().contains("z/OSMF JWT Check"));
    }

    @Test
    void whenZosmfJwtCheckFlagWithNoArgs_thenReturnsExitCode4() {
        String[] args = {"--zosmf-jwt-check"};
        assertEquals(4, Analyser.mainWithExitCode(args));
    }

    @Test
    void whenNoRemoteUrlProvided_thenMessageIsPrinted() {
        String[] args = {};
        assertEquals(4, Analyser.mainWithExitCode(args));
        assertTrue(outputStream.toString().contains("No remote will be verified."));
    }

    @Test
    void whenRemoteUrlProvidedWithoutClientCert_thenHandshakeIsAttempted() {
        String[] args = {"-r", "https://localhost:12345"};
        assertEquals(4, Analyser.mainWithExitCode(args));
        assertTrue(outputStream.toString().contains("Start of the remote SSL handshake."));
    }

    @Test
    void whenRemoteUrlProvidedWithClientCert_thenHandshakeIsAttempted() {
        String[] args = {
            "-r", "https://localhost:12345",
            "-c",
            "--keystore", "../keystore/service/service.keystore.p12",
            "--truststore", "../keystore/service/service.truststore.p12",
            "--keypasswd", "password"
        };
        assertEquals(4, Analyser.mainWithExitCode(args));
        assertTrue(outputStream.toString().contains("Start of the remote SSL handshake."));
    }

    @Test
    void whenInvalidKeystorePath_thenExceptionIsCaughtAndExitCodeIs4() {
        String[] args = {"--keystore", "invalid/path/to/keystore.p12"};
        assertEquals(4, Analyser.mainWithExitCode(args));
        assertTrue(errStream.toString().contains("Error while loading keystore file"));
    }

    @Nested
    class GivenEnsureSafkeyringHandler {

        private String originalProperty;

        @BeforeEach
        void saveProperty() {
            originalProperty = System.getProperty("java.protocol.handler.pkgs");
        }

        @AfterEach
        void restoreProperty() {
            if (originalProperty == null) {
                System.clearProperty("java.protocol.handler.pkgs");
            } else {
                System.setProperty("java.protocol.handler.pkgs", originalProperty);
            }
        }

        @Test
        void whenPropertyNotSet_thenBothPackagesAreRegistered() {
            System.clearProperty("java.protocol.handler.pkgs");
            org.zowe.apiml.common.KeyringUtils.ensureSafkeyringHandler();
            String value = System.getProperty("java.protocol.handler.pkgs");
            assertTrue(value.contains("com.ibm.crypto.zsecurity.provider"));
            assertTrue(value.contains("com.ibm.crypto.hdwrCCA.provider"));
        }

        @Test
        void whenPropertyAlreadyHasOtherPackages_thenNewPackagesAreAppended() {
            System.setProperty("java.protocol.handler.pkgs", "com.example.custom");
            org.zowe.apiml.common.KeyringUtils.ensureSafkeyringHandler();
            String value = System.getProperty("java.protocol.handler.pkgs");
            assertTrue(value.startsWith("com.example.custom|"));
            assertTrue(value.contains("com.ibm.crypto.zsecurity.provider"));
            assertTrue(value.contains("com.ibm.crypto.hdwrCCA.provider"));
        }

        @Test
        void whenPropertyAlreadyContainsPackages_thenNoDuplicatesAdded() {
            System.setProperty("java.protocol.handler.pkgs",
                "com.ibm.crypto.zsecurity.provider|com.ibm.crypto.hdwrCCA.provider");
            org.zowe.apiml.common.KeyringUtils.ensureSafkeyringHandler();
            String value = System.getProperty("java.protocol.handler.pkgs");
            assertEquals("com.ibm.crypto.zsecurity.provider|com.ibm.crypto.hdwrCCA.provider", value);
        }

        @Test
        void whenCalledViaMainWithExitCode_thenPropertyIsSet() {
            System.clearProperty("java.protocol.handler.pkgs");
            Analyser.mainWithExitCode(new String[]{});
            String value = System.getProperty("java.protocol.handler.pkgs");
            assertTrue(value.contains("com.ibm.crypto.zsecurity.provider"));
            assertTrue(value.contains("com.ibm.crypto.hdwrCCA.provider"));
        }
    }
}
