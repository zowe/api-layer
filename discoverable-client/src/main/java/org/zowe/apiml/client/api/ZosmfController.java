/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;

@RestController
public class ZosmfController {

    @RequestMapping("/zosmf/**")
    public void zosmfCall(WebRequest request, HttpServletRequest servletRequest) {
        System.out.println("Request to zOSMF");
        System.out.println(request.getParameterNames());
        System.out.println(request.getContextPath());
        System.out.println(servletRequest.getServletPath());
    }
}
