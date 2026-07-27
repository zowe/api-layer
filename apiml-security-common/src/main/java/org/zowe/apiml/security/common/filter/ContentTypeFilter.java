/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.filter;


import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Rejects requests that carry a body with HTTP 415 unless they declare a {@code Content-Type}
 * equals to {@code application/json}. Bodyless requests (e.g. cert or Basic-Auth login,
 * logout) pass through unchecked, since they have nothing to be misinterpreted as JSON.
 * <p>
 * Body presence is determined from the {@code Content-Length}/{@code Transfer-Encoding} headers
 * rather than by reading the request body, since the body stream is only safely readable once
 * downstream (e.g. by {@code LoginFilter}).
 */
public class ContentTypeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        boolean hasBody = request.getContentLengthLong() > 0 || request.getHeader(HttpHeaders.TRANSFER_ENCODING) != null;
        if (hasBody && !hasJsonContentType(request)) {
            response.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean hasJsonContentType(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        try {
           MediaType mediaType = MediaType.parseMediaType(contentType);
            return MediaType.APPLICATION_JSON.equalsTypeAndSubtype(mediaType);
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    }

}
