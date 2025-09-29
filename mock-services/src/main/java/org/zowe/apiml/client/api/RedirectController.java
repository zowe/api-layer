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

import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpHeaders.LOCATION;

@RestController
public class RedirectController {

    /**
     * Get url from POST request body, then set the url to Location response header, and set status code to 307
     *
     * @param redirectLocation request body which contains a url
     * @param response         return the same data as request body
     * @return
     */
    @PostMapping(
        value = "/api/v1/redirect",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.FOUND)
    public RedirectLocation redirectPage(@RequestBody RedirectLocation redirectLocation,
                                         HttpServletResponse response) {
        response.setHeader(LOCATION, redirectLocation.getLocation());
        return redirectLocation;
    }

    @Data
    static class RedirectLocation {
        private String location;
    }
}
