/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.vsam;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


class VsamUtilsTest {

    @Test
    void padStringToLength() {
        assertThat(VsamUtils.padToLength("Princess", 10), is("Princess  "));
        assertThat(VsamUtils.padToLength("Princess", 3), is("Princess"));
    }
}
