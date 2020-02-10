/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.interceptors;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

public class HttpInterceptor implements HandlerInterceptor {

    private static final List<String> PROHIBITED_CHARACTERS = Arrays.asList("%2f", "%2F", "\\", "%5c", "%5C");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        boolean encodedCharactersAllowed = false;
        if (!encodedCharactersAllowed && checkRequestForEncodedCharacters(request.getRequestURI())) {
            rejectRequest();        }
        return true;
    }

    private boolean checkRequestForEncodedCharacters(String request) {
        return PROHIBITED_CHARACTERS.stream()
            .anyMatch(forbidden -> pathContains(request, forbidden));
    }

    private void rejectRequest() {
        throw new IllegalArgumentException("Gateway does not allow encoded slashes in the URL.");
    }

    private static boolean pathContains(String path, String character) {
        return path != null && path.contains(character);
    }
}
