/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance;

import org.zowe.apiml.auth.AuthenticationScheme;

public class ZosmfSchemeTest extends TokenSchemeTest {

    @Override
    public String getTokenEndpoint() {
        return "/zaas/scheme/zosmf";
    }

    @Override
    public AuthenticationScheme getAuthenticationScheme() {
        return AuthenticationScheme.ZOSMF;
    }

}
