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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.util.CookieUtil;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper.IGNORED_HEADERS;
import static org.zowe.apiml.gateway.security.service.schema.JwtCommand.COOKIE_HEADER;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;

@RequiredArgsConstructor
public class OidcCommand extends AuthenticationCommand {


    private final String token;

    @Override
    public void apply(InstanceInfo instanceInfo) {
        RequestContext context = RequestContext.getCurrentContext();

        String cookie = context.getRequest().getHeader(COOKIE_HEADER);
        if (!StringUtils.isEmpty(cookie)) {
            cookie = CookieUtil.removeCookie(cookie, COOKIE_AUTH_NAME);
            if (StringUtils.isEmpty(cookie)) {
                removeCookieFromRequest(context, COOKIE_HEADER);
            } else {
                context.addZuulRequestHeader(COOKIE_HEADER, cookie);
            }
        }
        removeCookieFromRequest(context, HttpHeaders.AUTHORIZATION);

        context.addZuulRequestHeader(ApimlConstants.HEADER_OIDC_TOKEN, token);
    }

    void removeCookieFromRequest(RequestContext context, String headerName) {
        ((Set<String>) context.computeIfAbsent(IGNORED_HEADERS, k -> new HashSet<>())).add(headerName.toLowerCase()); // NOSONAR
    }

}
