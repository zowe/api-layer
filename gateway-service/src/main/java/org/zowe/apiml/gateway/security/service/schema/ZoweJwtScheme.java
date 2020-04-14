/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.schema;

import org.springframework.stereotype.Component;
import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.util.function.Supplier;

@Component
public class ZoweJwtScheme implements AbstractAuthenticationScheme {

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.ZOWE_JWT;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, Supplier<QueryResponse> token) {
        return AuthenticationCommand.EMPTY;
    }

}
