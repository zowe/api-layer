/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.error;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.auth.saf.PlatformReturned;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ZosAuthenticationExceptionTest {

    private final PlatformReturned returned = PlatformReturned.builder().errno(PlatformPwdErrno.EACCES.errno).build();
    private final ZosAuthenticationException underTest = new ZosAuthenticationException(returned);

    @Nested
    class GivenPlatformReturned {

        @Test
        void messageSourcedFromEnum() {
            assertThat(underTest.getMessage(),
                is(PlatformPwdErrno.EACCES.shortErrorName
                    + ": " + PlatformPwdErrno.EACCES.explanation));
        }

        @Test
        void enumValueCanBeRetrieved() {
            assertThat(underTest.getPlatformError(), is(PlatformPwdErrno.EACCES));
        }
    }

}
