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
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;


@Component
public class ZoweJwtScheme implements AbstractAuthenticationScheme {

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.ZOWE_JWT;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        return AuthenticationCommand.EMPTY;
    }

    public class ZoweJwtAuthCommand extends AuthenticationCommand {

        public static final long serialVersionUID = -885301934611866658L;
        public static final String COOKIE_HEADER = "cookie";
        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();

        }
    }

}
