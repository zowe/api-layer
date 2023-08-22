/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.service.scheme;

import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;

import java.util.HashMap;
import java.util.Map;

@Component
public class X509 implements SchemeHandler {

    @Override
    public AuthenticationScheme getAuthenticationScheme() {
        return AuthenticationScheme.X509;
    }

    @Override
    public void apply(RouteDefinition routeDefinition, Authentication auth) {
        FilterDefinition x509filter = new FilterDefinition();
        x509filter.setName("X509FilterFactory");
        Map<String,String> m = new HashMap<>();
        m.put("headers", auth.getHeaders());
        x509filter.setArgs(m);
        routeDefinition.getFilters().add(x509filter);
    }

}
