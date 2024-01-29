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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.client.model.X509SchemeResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class X509Controller {

    @GetMapping("/api/v1/x509")
    public X509SchemeResponse getValueFromHeader(HttpServletRequest request) {
        return new X509SchemeResponse(
            request.getHeader("X-Certificate-Public"),
            request.getHeader("X-Certificate-DistinguishedName"),
            request.getHeader("X-Certificate-CommonName")
        );

    }

}
