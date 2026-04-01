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

import static org.junit.jupiter.api.Assertions.*;

class ZosmfJwtCheckTest {

    private final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setupStreams() {
        System.setOut(new PrintStream(outStream));
        System.setErr(new PrintStream(errStream));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void helpFlagReturnsExitCode8() {
        String[] args = {"--help"};
        assertEquals(8, ZosmfJwtCheck.mainWithExitCode(args));
        assertTrue(outStream.toString().contains("z/OSMF JWT Check"));
    }

    @Test
    void missingRequiredArgsReturnsExitCode4() {
        String[] args = {};
        assertEquals(4, ZosmfJwtCheck.mainWithExitCode(args));
    }

    @Nested
    class ValidationTests {

        @Test
        void invalidSchemeIsRejected() {
            String[] args = {"--zosmf-host", "localhost", "--zosmf-port", "443", "--scheme", "ftp"};
            assertEquals(4, ZosmfJwtCheck.mainWithExitCode(args));
            assertTrue(errStream.toString().contains("--scheme must be 'http' or 'https'"));
        }

        @Test
        void invalidVerifyCertificatesIsRejected() {
            String[] args = {"--zosmf-host", "localhost", "--zosmf-port", "443", "--scheme", "https",
                "--truststore", "some/path.p12", "--truststore-password", "pass",
                "--verify-certificates", "INVALID"};
            assertEquals(4, ZosmfJwtCheck.mainWithExitCode(args));
            assertTrue(errStream.toString().contains("--verify-certificates must be STRICT, NONSTRICT, or DISABLED"));
        }

        @Test
        void httpsStrictWithoutTruststoreIsRejected() {
            String[] args = {"--zosmf-host", "localhost", "--zosmf-port", "443", "--scheme", "https"};
            assertEquals(4, ZosmfJwtCheck.mainWithExitCode(args));
            assertTrue(errStream.toString().contains("--truststore is required"));
        }

        @Test
        void httpsNonstrictWithoutTruststoreIsRejected() {
            String[] args = {"--zosmf-host", "localhost", "--zosmf-port", "443", "--scheme", "https",
                "--verify-certificates", "NONSTRICT"};
            assertEquals(4, ZosmfJwtCheck.mainWithExitCode(args));
            assertTrue(errStream.toString().contains("--truststore is required"));
        }

        @Test
        void httpsWithoutTruststorePasswordIsRejected() {
            String[] args = {"--zosmf-host", "localhost", "--zosmf-port", "443", "--scheme", "https",
                "--truststore", "some/path.p12"};
            assertEquals(4, ZosmfJwtCheck.mainWithExitCode(args));
            assertTrue(errStream.toString().contains("--truststore-password is required"));
        }

        @Test
        void httpsDisabledDoesNotRequireTruststore() {
            // DISABLED mode skips certificate verification entirely — no truststore needed
            // Will fail to connect to a non-existent server, but should pass validation
            String[] args = {"--zosmf-host", "localhost", "--zosmf-port", "19999", "--scheme", "https",
                "--verify-certificates", "DISABLED"};
            int exitCode = ZosmfJwtCheck.mainWithExitCode(args);
            assertEquals(4, exitCode);
            assertFalse(errStream.toString().contains("--truststore is required"));
        }

        @Test
        void httpDoesNotRequireTruststore() {
            // This will fail to connect but should not fail validation
            String[] args = {"--zosmf-host", "localhost", "--zosmf-port", "19999", "--scheme", "http"};
            int exitCode = ZosmfJwtCheck.mainWithExitCode(args);
            // Should be 4 (connection failure) not a validation error
            assertEquals(4, exitCode);
            assertFalse(errStream.toString().contains("--truststore is required"));
        }
    }
}
