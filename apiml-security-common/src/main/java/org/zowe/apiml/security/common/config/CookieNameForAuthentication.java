/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class CookieNameForAuthentication {

    public static String cookieName;

    public static final String COOKIE_AUTH_NAME = "apimlAuthenticationToken";

    @Value("${apiml.security.auth.jwt.cookieName:apimlAuthenticationToken}")
    public void setCookieName(String cookieName) {

        if (System.getProperty("COOKIE_NAME") == null) {
            System.setProperty("COOKIE_NAME", cookieName);
            CookieNameForAuthentication.cookieName = cookieName;
        }
    }

    public String getCookieName() {
        if (cookieName == null) {
            return COOKIE_AUTH_NAME;
        }
        return cookieName;
    }
}
