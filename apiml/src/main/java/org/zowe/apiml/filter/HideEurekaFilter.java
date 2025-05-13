/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.core.MessageService;

import java.io.IOException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

/**
 * In modulith, all requests are handled by the same filters, on a separate connector.
 * This filter makes sure /eureka endpoints are not accesible from the external GW port.
 */
@Component
public class HideEurekaFilter extends PreFluxFilter {

    private final String error404Message;

    public HideEurekaFilter(MessageService messageService) throws JsonProcessingException {
        super(Type.EXTERNAL_ONLY);
        error404Message = new ObjectMapper().writeValueAsString(
            messageService.createMessage("org.zowe.apiml.common.notFound").mapToView()
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (
            StringUtils.equals(request.getRequestURI(), "/eureka") ||
            StringUtils.startsWith(request.getRequestURI(), "/eureka/")
        ) {
            response.addHeader(CONTENT_TYPE, APPLICATION_JSON);
            response.getOutputStream().print(error404Message);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            chain.doFilter(request, response);
        }
    }

}
