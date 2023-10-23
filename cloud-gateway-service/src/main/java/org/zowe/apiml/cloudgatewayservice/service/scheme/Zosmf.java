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

@Component
public class Zosmf implements SchemeHandler {

    @Override
    public AuthenticationScheme getAuthenticationScheme() {
        return AuthenticationScheme.ZOSMF;
    }

    @Override
    public void apply(RouteDefinition routeDefinition, Authentication auth) {
        FilterDefinition filerDef = new FilterDefinition();
        filerDef.setName("ZosmfTokensFilterFactory");
        routeDefinition.getFilters().add(filerDef);
    }

}
