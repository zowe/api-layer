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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;

@Slf4j
class ApimlRollingFileAppenderTest {
    ApimlRollingFileAppender<Object> underTest;

    @BeforeEach
    void setUp() {
        underTest = new ApimlRollingFileAppender<>();
    }

    @Test
    void givenLogLevelAndWorkspaceDirectory_whenTheApplicationStarts_thenTheParamtersAreVerified() {
        System.setProperty("spring.profiles.include", "debug");
        System.setProperty("apiml.logs.location", "validLocation");

        boolean result = underTest.verifyStartupParams();
        assertThat(result, is(true));
    }

    @Test
    void givenNullLogLevelAndWorkspaceDirectory_whenTheApplicationStarts_thenTheLoggerDoesntStart() {
        System.setProperty("apiml.logs.location", "validLocation");
        System.setProperty("spring.profiles.include", "");

        boolean result = underTest.verifyStartupParams();
        assertThat(result, is(false));
    }

    @Test
    void givenLogLevelAndNullWorkspaceDirectory_whenTheApplicationStarts_thenTheLoggerDoesntStart() {
        System.setProperty("apiml.logs.location", "");
        System.setProperty("spring.profiles.include", "debug");

        boolean result = underTest.verifyStartupParams();
        assertThat(result, is(false));
    }

    @Test
    void givenLogLevelAndWorkspaceDirectory_whenTheApplicationStarts_thenTheLoggerStarts() {
        System.setProperty("spring.profiles.include", "debug");
        System.setProperty("apiml.logs.location", "validLocation");

        TimeBasedRollingPolicy<Object> tbrp = new TimeBasedRollingPolicy<>();
        Context context = new ContextBase();
        underTest.setEncoder(mock(Encoder.class));
        underTest.setName("test");
        tbrp.setContext(context);
        tbrp.setParent(underTest);
        underTest.setContext(context);
        tbrp.setFileNamePattern("target/test-output/toto-%d.log");
        tbrp.start();
        underTest.setRollingPolicy(tbrp);

        underTest.start();
        assertThat(underTest.isStarted(), is(true));
    }
}
