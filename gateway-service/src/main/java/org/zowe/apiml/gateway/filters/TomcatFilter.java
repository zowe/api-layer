/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.io.IOException;
import java.net.URLDecoder;

/**
 * This filter checks if encoded slashes in the URI are allowed based on configuration.
 * If not allowed and encoded slashes are present, it returns a BAD_REQUEST response.
 */
@Component
@WebFilter
@RequiredArgsConstructor
public class TomcatFilter implements Filter {

    private final MessageService messageService;
    private final ObjectMapper mapper;

    @Value("${apiml.service.allowEncodedSlashes:#{true}}")
    private boolean allowEncodedSlashes;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    /**
     * Filters requests to check for encoded slashes in the URI.
     * If encoded slashes are not allowed and found, returns a BAD_REQUEST response.
     * Otherwise, proceeds with the filter chain.
     *
     * @param request  The servlet request
     * @param response The servlet response
     * @param chain    The filter chain
     * @throws IOException      If an I/O error occurs
     * @throws ServletException If a servlet-specific error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();
        String decodedUri = URLDecoder.decode(uri, "UTF-8");
        final boolean isRequestEncoded = !uri.equals(decodedUri);

        Message message = messageService.createMessage("org.zowe.apiml.gateway.requestContainEncodedSlash", uri);
        if (!allowEncodedSlashes && isRequestEncoded) {
            res.setStatus(HttpStatus.BAD_REQUEST.value());
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            try {
                mapper.writeValue(res.getWriter(), message.mapToView());
            } catch (IOException e) {
                apimlLog.log("org.zowe.apiml.security.errorWrittingResponse", e.getMessage());
                throw new ServletException("Error writing response", e);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
