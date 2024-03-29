/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance;

import org.zowe.apiml.auth.AuthenticationScheme;

public class ZosmfSchemeTest extends TokenSchemeTest {

    @Override
    public String getTokenEndpoint() {
        return "/gateway/zaas/zosmf";
    }

    @Override
    public AuthenticationScheme getAuthenticationScheme() {
        return AuthenticationScheme.ZOSMF;
    }

}
