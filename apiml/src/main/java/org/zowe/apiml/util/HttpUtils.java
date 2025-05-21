/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

@Component
@RequiredArgsConstructor
public class HttpUtils {

    private final AuthConfigurationProperties authConfigurationProperties;

    public ResponseCookie createResponseCookie(String jwt) {
        AuthConfigurationProperties.CookieProperties cp = authConfigurationProperties.getCookieProperties();

        // Create the HttpOnly cookie containing the JWT
        return ResponseCookie.from(cp.getCookieName(), jwt)
            .path(cp.getCookiePath())
            .sameSite(cp.getCookieSameSite().getValue())
            .maxAge(cp.getCookieMaxAge() != null ? cp.getCookieMaxAge() : -1)
            .httpOnly(true)
            .secure(cp.isCookieSecure())
            .build();
    }
}
