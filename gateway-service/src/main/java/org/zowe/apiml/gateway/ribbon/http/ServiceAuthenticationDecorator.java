/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.http;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpRequest;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationCommand;
import org.zowe.apiml.gateway.security.service.schema.ServiceAuthenticationService;
import org.zowe.apiml.security.common.auth.Authentication;

import static org.zowe.apiml.gateway.ribbon.ApimlZoneAwareLoadBalancer.LOADBALANCED_INSTANCE_INFO_KEY;

// TODO do we need interface or abstraction?
// TODO add tests
@RequiredArgsConstructor
public class ServiceAuthenticationDecorator {

    private final ServiceAuthenticationService serviceAuthenticationService;
    private final AuthenticationService authenticationService;

    private static final String AUTHENTICATION_COMMAND_KEY = "zoweAuthenticationCommand";

    public void process(HttpRequest request) {
        RequestContext context = RequestContext.getCurrentContext();
        InstanceInfo info = (InstanceInfo) context.get(LOADBALANCED_INSTANCE_INFO_KEY);
        if (context.get(AUTHENTICATION_COMMAND_KEY) != null && context.get(AUTHENTICATION_COMMAND_KEY) instanceof AuthenticationCommand) {
            Authentication authentication = serviceAuthenticationService.getAuthentication(info);
            String jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest()).orElse(null);
            AuthenticationCommand cmd = serviceAuthenticationService.getAuthenticationCommand(authentication, jwtToken);
            cmd.applyToRequest(request);
        }
    }
}

