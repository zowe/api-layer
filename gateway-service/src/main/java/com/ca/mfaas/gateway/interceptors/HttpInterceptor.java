/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.interceptors;

import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

public class HttpInterceptor extends HandlerInterceptorAdapter {

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Value("${apiml.service.allowEncodedCharacters}")
    private Boolean allowEncodedCharacters;

    private static final List<String> PROHIBITED_CHARACTERS = Arrays.asList("%2e", "%2E", ";", "%3b", "%3B", "%2f", "%2F", "\\", "%5c", "%5C", "%25", "%");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        boolean encodedCharactersAllowed = allowEncodedCharacters;
        if (!encodedCharactersAllowed && checkRequestForEncodedCharacters(request.getRequestURI())) {
            rejectRequest();
        }
        return true;
    }

    private boolean checkRequestForEncodedCharacters(String request) {
        return PROHIBITED_CHARACTERS.stream()
            .anyMatch(forbidden -> pathContains(request, forbidden));
    }

    private void rejectRequest() {
        throw new IllegalArgumentException("Instance ... does not allow encoded characters in the URL.");
    }

    private static boolean pathContains(String path, String character) {
        return path != null && path.contains(character);
    }
}
