/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggingSetupTest {

    private static final String LOGS_LOCATION_PROPERTY = "apiml.logs.location";

    private static final Path LOGS_LOCATION;

    static {
        // Logback is configured while the Spring context starts, so the location must be set before that
        try {
            LOGS_LOCATION = Files.createTempDirectory("apiml-logging-setup-test");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.setProperty(LOGS_LOCATION_PROPERTY, LOGS_LOCATION.toString());
    }

    @AfterAll
    static void cleanUp() throws IOException {
        System.clearProperty(LOGS_LOCATION_PROPERTY);
        try (Stream<Path> files = Files.walk(LOGS_LOCATION)) {
            files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    private static final String TEST_LOGGER_PROPERTY = "logging.level.org.zowe.apiml.acceptance.debugonly=TRACE";

    private static final org.slf4j.Logger TEST_LOGGER = LoggerFactory.getLogger("org.zowe.apiml.acceptance.debugonly");

    /**
     * Logs messages on all levels and returns what was written to the standard output.
     */
    private static String logAndCaptureStdout(String marker) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream replacement = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(replacement);
            TEST_LOGGER.trace("{} TRACE message", marker);
            TEST_LOGGER.debug("{} DEBUG message", marker);
            TEST_LOGGER.info("{} INFO message", marker);
            TEST_LOGGER.warn("{} WARN message", marker);
        } finally {
            System.setOut(originalOut);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    private static String readLogFile() throws IOException {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Appender<ILoggingEvent> fileAppender = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("FILE");
        assertThat(fileAppender).isInstanceOf(FileAppender.class);
        return Files.readString(Path.of(((FileAppender<ILoggingEvent>) fileAppender).getFile()));
    }

    @Nested
    @AcceptanceTest
    @TestPropertySource(properties = {
        "logging.config=classpath:logback-spring.xml",
        "apiml.logging.toFile.enabled=false"
    })
    class FileAppenderDisabledTest {

        @Test
        void verifyFileAppenderIsNotAttached() {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

            assertThat(loggerContext.getProperty("LOG_TO_FILE")).isEqualTo("false");

            Appender<ILoggingEvent> fileAppender = rootLogger.getAppender("FILE");

            // Verify FILE appender is NOT attached to root logger
            assertThat(fileAppender)
                .as("FILE appender should not be attached when logging to file is disabled")
                .isNull();

            // STDOUT appender should remain active
            assertThat(rootLogger.getAppender("STDOUT")).isNotNull();
        }
    }

    @Nested
    @AcceptanceTest
    @TestPropertySource(properties = {
        "logging.config=classpath:logback-spring.xml",
        "apiml.logging.toFile.enabled=true"
    })
    class FileAppenderEnabledTest {

        @Test
        void verifyFileAppenderIsAttachedAndStarted() {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

            assertThat(loggerContext.getProperty("LOG_TO_FILE")).isEqualTo("true");

            Appender<ILoggingEvent> fileAppender = rootLogger.getAppender("FILE");

            assertThat(fileAppender)
                .as("FILE appender should be attached when logging to file is enabled")
                .isNotNull();

            assertThat(fileAppender.isStarted())
                .as("FILE appender should be started")
                .isTrue();
        }
    }

    @Nested
    @AcceptanceTest
    @TestPropertySource(properties = {
        "logging.config=classpath:logback-spring.xml",
        TEST_LOGGER_PROPERTY,
        "apiml.logging.toFile.enabled=true",
        "apiml.logging.toFile.debugOnly=true"
    })
    class DebugOnlyToFileEnabledTest {

        @Test
        void verifyDebugAndTraceMessagesAreWrittenOnlyToFile() throws IOException {
            String marker = "debugOnly-enabled";
            String stdout = logAndCaptureStdout(marker);
            String file = readLogFile();

            assertThat(stdout)
                .doesNotContain(marker + " TRACE message", marker + " DEBUG message")
                .contains(marker + " INFO message", marker + " WARN message");
            assertThat(file)
                .contains(marker + " TRACE message", marker + " DEBUG message", marker + " INFO message", marker + " WARN message");
        }
    }

    @Nested
    @AcceptanceTest
    @TestPropertySource(properties = {
        "logging.config=classpath:logback-spring.xml",
        TEST_LOGGER_PROPERTY,
        "apiml.logging.toFile.enabled=true",
        "apiml.logging.toFile.debugOnly=false"
    })
    class DebugOnlyToFileDisabledTest {

        @Test
        void verifyDebugAndTraceMessagesAreWrittenToStdoutAndFile() throws IOException {
            String marker = "debugOnly-disabled";
            String stdout = logAndCaptureStdout(marker);
            String file = readLogFile();

            assertThat(stdout)
                .contains(marker + " TRACE message", marker + " DEBUG message", marker + " INFO message", marker + " WARN message");
            assertThat(file)
                .contains(marker + " TRACE message", marker + " DEBUG message", marker + " INFO message", marker + " WARN message");
        }
    }
}
