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

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

@Slf4j
public class ApimlRollingFileAppenderTest {
    ApimlRollingFileAppender underTest;

    @Before
    public void setUp() {
        underTest = new ApimlRollingFileAppender();
    }

    @Test
    public void givenLogLevelAndWorkspaceDirectory_whenTheApplicationStarts_thenTheLoggerStarts() {
        System.setProperty("spring.profiles.include", "debug");
        System.setProperty("apiml.logs.location", "validLocation");

        boolean result = underTest.verifyStartupParams();
        assertThat(result, is(true));
    }

    @Test
    public void givenNullLogLevelAndWorkspaceDirectory_whenTheApplicationStarts_thenTheLoggerDoesntStart() {
        System.setProperty("apiml.logs.location", "validLocation");
        System.setProperty("spring.profiles.include", "");

        boolean result = underTest.verifyStartupParams();
        assertThat(result, is(false));
    }

    @Test
    public void givenLogLevelAndNullWorkspaceDirectory_whenTheApplicationStarts_thenTheLoggerDoesntStart() {
        System.setProperty("apiml.logs.location", "");
        System.setProperty("spring.profiles.include", "debug");

        boolean result = underTest.verifyStartupParams();
        assertThat(result, is(false));
    }
}
