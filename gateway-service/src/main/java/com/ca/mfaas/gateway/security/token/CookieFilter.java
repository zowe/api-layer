/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.token;

import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class CookieFilter extends AbstractSecureContentFilter {
    private final MFaaSConfigPropertiesContainer propertiesContainer;

    public CookieFilter(AuthenticationManager authenticationManager,
                        AuthenticationFailureHandler failureHandler,
                        MFaaSConfigPropertiesContainer propertiesContainer) {
        super(authenticationManager, failureHandler);
        this.propertiesContainer = propertiesContainer;
    }

    @Override
    protected String extractContent(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(propertiesContainer.getSecurity().getCookieProperties().getCookieName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
