/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.config;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

class ApiInfoTest {
    @Test
    void whenThereIsNoVersionReturnMinusOne() {
        ApiInfo underTest = new ApiInfo("org.zowe", "api/v1", null, "swaggerUrl", "documentationUrl");

        assertThat(underTest.getMajorVersion(), is(-1));
    }

    @Test
    void whenThereIsMajorVersionReturnIt() {
        ApiInfo underTest = new ApiInfo("org.zowe", "api/v1", "1.0.0", "swaggerUrl", "documentationUrl");

        assertThat(underTest.getMajorVersion(), is(1));
    }
}
