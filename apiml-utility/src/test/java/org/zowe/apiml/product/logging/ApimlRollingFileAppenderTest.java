/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.logging;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;

@Slf4j
class ApimlRollingFileAppenderTest {
    private static final String LOGS_LOCATION_PROPERTY = "apiml.logs.location";

    ApimlRollingFileAppender<Object> underTest;
    Context context;

    @BeforeEach
    void setUp() {
        underTest = new ApimlRollingFileAppender<>();
        context = new ContextBase();
        underTest.setContext(context);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(LOGS_LOCATION_PROPERTY);
    }

    @Test
    void givenFileLoggingEnabledAndWorkspaceDirectory_whenTheApplicationStarts_thenTheParamtersAreVerified() {
        context.putProperty("LOG_TO_FILE", "true");
        System.setProperty(LOGS_LOCATION_PROPERTY, "validLocation");

        boolean result = underTest.verifyStartupParams();
        assertThat(result, is(true));
    }

    @Test
    void givenFileLoggingDisabledAndWorkspaceDirectory_whenTheApplicationStarts_thenTheLoggerDoesntStart() {
        context.putProperty("LOG_TO_FILE", "false");
        System.setProperty(LOGS_LOCATION_PROPERTY, "validLocation");

        boolean result = underTest.verifyStartupParams();
        assertThat(result, is(false));
    }

    @Test
    void givenFileLoggingNotConfiguredInContext_whenTheApplicationStarts_thenTheLoggerDoesntStart() {
        System.setProperty(LOGS_LOCATION_PROPERTY, "validLocation");

        boolean result = underTest.verifyStartupParams();
        assertThat(result, is(false));
    }

    @Test
    void givenFileLoggingEnabledOnlyAsSystemProperty_whenTheApplicationStarts_thenTheLoggerDoesntStart() {
        System.setProperty("apiml.logging.toFile.enabled", "true");
        System.setProperty(LOGS_LOCATION_PROPERTY, "validLocation");
        try {
            boolean result = underTest.verifyStartupParams();
            assertThat(result, is(false));
        } finally {
            System.clearProperty("apiml.logging.toFile.enabled");
        }
    }

    @Test
    void givenNoContext_whenTheApplicationStarts_thenTheLoggerDoesntStart() {
        ApimlRollingFileAppender<Object> withoutContext = new ApimlRollingFileAppender<>();
        System.setProperty(LOGS_LOCATION_PROPERTY, "validLocation");

        boolean result = withoutContext.verifyStartupParams();
        assertThat(result, is(false));
    }

    @Test
    void givenFileLoggingEnabledAndNullWorkspaceDirectory_whenTheApplicationStarts_thenTheLoggerDoesntStart() {
        context.putProperty("LOG_TO_FILE", "true");
        System.setProperty(LOGS_LOCATION_PROPERTY, "");

        boolean result = underTest.verifyStartupParams();
        assertThat(result, is(false));
    }

    @Test
    void givenFileLoggingEnabledAndWorkspaceDirectory_whenTheApplicationStarts_thenTheLoggerStarts() {
        context.putProperty("LOG_TO_FILE", "true");
        System.setProperty(LOGS_LOCATION_PROPERTY, "validLocation");

        TimeBasedRollingPolicy<Object> tbrp = new TimeBasedRollingPolicy<>();
        underTest.setEncoder(mock(Encoder.class));
        underTest.setName("test");
        tbrp.setContext(context);
        tbrp.setParent(underTest);
        tbrp.setFileNamePattern("target/test-output/toto-%d.log");
        tbrp.start();
        underTest.setRollingPolicy(tbrp);

        underTest.start();
        assertThat(underTest.isStarted(), is(true));
    }
}
