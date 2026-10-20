/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.zowe.apiml.util.Recorder;
import org.zowe.apiml.util.TestLogger;

import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CachingServiceApplicationTest {

    private Recorder<ILoggingEvent> recorder = TestLogger.getHandler();

    @BeforeAll
    void startRecording() {
        recorder.startRecording();
    }

    @AfterAll
    void stopRecording() {
        recorder.stopRecording();
    }

    @Nested
    @SpringBootTest(
        properties = {
            "logging.config=classpath:logback-testlogger.xml",
            "apiml.enabled=false",
            "apiml.security.httpRedirect.enabled=true",
            "logging.level.org.zowe.apiml.product.web=DEBUG",
            "logging.level.org.zowe.apiml.product.security=DEBUG"
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    @ActiveProfiles({"test", "debug"})
    class TomcatInitialization {

        @LocalServerPort
        private int port;

        @Test
        void redirectsPlaintextOnTheRandomTlsPort() throws Exception {
            try (var socket = new Socket("localhost", port)) {
                socket.setSoTimeout(5000);
                socket.getOutputStream().write("GET /application/info HTTP/1.1\r\nHost: localhost\r\n\r\n"
                    .getBytes(StandardCharsets.US_ASCII));
                var response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
                assertTrue(response.startsWith("HTTP/1.1 308"));
                assertTrue(response.contains("Location: https://localhost:" + port + "/application/info\r\n"));
            }
        }

        @ParameterizedTest(name = "Check if {0} is initialized on startup")
        @CsvSource({
            "ServletContainerCustomizer,servletContainerCustomizer initialized",
            "TomcatKeyringFix,TomcatKeyringFix applied",
            "TomcatAcceptFixConfig,TomcatAcceptFixConfig applied"
        })
        void givenStandardConfiguration_whenServiceStarted_thenComponentIsInitialized(
            String componentName, String logMessage
        ) {
            assertFalse(recorder.find(logMessage).isEmpty());
        }

    }

}
