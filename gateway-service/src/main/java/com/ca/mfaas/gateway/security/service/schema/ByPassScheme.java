/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.service.schema;

import com.ca.apiml.security.common.auth.Authentication;
import com.ca.apiml.security.common.auth.AuthenticationScheme;
import com.ca.apiml.security.common.token.QueryResponse;
import org.springframework.stereotype.Component;

/**
 * Default scheme, just forward, don't set anything.
 */
@Component
public class ByPassScheme implements AbstractAuthenticationScheme {

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.BYPASS;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, QueryResponse token) {
        return AuthenticationCommand.EMPTY;
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}
