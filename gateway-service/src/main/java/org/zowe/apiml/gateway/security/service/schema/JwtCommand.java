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

import com.netflix.zuul.context.RequestContext;
import org.zowe.apiml.util.CookieUtil;
import org.zowe.apiml.util.Cookies;

import java.net.HttpCookie;

public abstract class JwtCommand extends AuthenticationCommand {

    public static final String COOKIE_HEADER = "cookie";

    public static void createCookie(Cookies cookies, String name, String token) {
        HttpCookie jwtCookie = new HttpCookie(name, token);
        jwtCookie.setSecure(true);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setVersion(0);
        cookies.set(jwtCookie);
    }

    public static void setCookie(RequestContext context, String name, String value) {
        context.addZuulRequestHeader(COOKIE_HEADER,
            CookieUtil.setCookie(
                context.getRequest().getHeader(COOKIE_HEADER),
                name,
                value
            )
        );
    }

    public static void removeCookie(RequestContext context, String name) {
        context.addZuulRequestHeader(COOKIE_HEADER,
            CookieUtil.removeCookie(
                context.getRequest().getHeader(COOKIE_HEADER),
                name
            )
        );
    }

    @Override
    public boolean isExpired() {
        if (getExpireAt() == null) return false;

        return System.currentTimeMillis() > getExpireAt();
    }

    @Override
    public boolean isRequiredValidSource() {
        return true;
    }

    public abstract Long getExpireAt();
}
