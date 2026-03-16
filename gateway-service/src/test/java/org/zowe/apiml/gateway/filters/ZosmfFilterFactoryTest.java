/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.auth.AuthenticationScheme;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ZosmfFilterFactoryTest {

    @Test
    void givenZosmfFilterFactory_whenGetAuthenticationScheme_thenReturnZosmf() {
        var zosmfFilterFactory = new ZosmfFilterFactory(null, null, null);
        assertEquals(AuthenticationScheme.ZOSMF, zosmfFilterFactory.getAuthenticationScheme());
    }

}
