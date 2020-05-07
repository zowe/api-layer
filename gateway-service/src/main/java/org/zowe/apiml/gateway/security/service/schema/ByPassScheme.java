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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import org.apache.http.HttpRequest;
import org.springframework.stereotype.Component;
import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.util.function.Supplier;

/**
 * Default scheme, just forward, don't set anything.
 */
@Component
public class ByPassScheme implements AbstractAuthenticationScheme {

    public static final String AUTHENTICATION_SCHEME_BY_PASS_KEY = "AuthenticationSchemeByPass";

    private static final AuthenticationCommand AUTHENTICATION_COMMAND = new AuthenticationCommand() {

        private static final long serialVersionUID = -3351658649447418579L;

        @Override
        public boolean isExpired() {
            return false;
        }

        @Override
        public void apply(InstanceInfo instanceInfo) {
            RequestContext.getCurrentContext().put(AUTHENTICATION_SCHEME_BY_PASS_KEY, Boolean.TRUE);
        }

        @Override
        public void applyToRequest(HttpRequest request) {
            // do nothing
        }

        @Override
        public boolean isRequiredValidJwt() {
            return false;
        }
    };

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.BYPASS;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, Supplier<QueryResponse> token) {
        return AUTHENTICATION_COMMAND;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

}
