/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Stores the address of the TCP peer of a Eureka request into {@link RegistrationRequestContext}
 */
@Slf4j
public class RegistrationRequestContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        InetAddress peerAddress;
        try {
            // remote address is an IP literal, no name resolution is made
            peerAddress = InetAddress.getByName(request.getRemoteAddr());
        } catch (UnknownHostException e) {
            log.debug("Invalid remote address {}", request.getRemoteAddr(), e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        RegistrationRequestContext.setPeerAddress(peerAddress);
        try {
            filterChain.doFilter(request, response);
        } finally {
            RegistrationRequestContext.clear();
        }
    }

}
