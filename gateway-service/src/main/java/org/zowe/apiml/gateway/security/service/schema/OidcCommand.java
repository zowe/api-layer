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
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.zowe.apiml.util.CookieUtil;

import static org.zowe.apiml.gateway.security.service.schema.JwtCommand.COOKIE_HEADER;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;

@RequiredArgsConstructor
public class OidcCommand extends AuthenticationCommand {

    private final String token;

    @Override
    public void apply(InstanceInfo instanceInfo) {
        RequestContext context = RequestContext.getCurrentContext();

        // remove apimlAuthenticationToken cookie
        String cookie = context.getRequest().getHeader(COOKIE_HEADER);
        cookie = CookieUtil.removeCookie(cookie, COOKIE_AUTH_NAME);
        context.addZuulRequestHeader(COOKIE_HEADER, cookie);

        // remove authorization header
        context.getZuulRequestHeaders().remove(HttpHeaders.AUTHORIZATION);

        // set OIDC header
        context.addZuulRequestHeader("OIDC-token", token);
    }

}
