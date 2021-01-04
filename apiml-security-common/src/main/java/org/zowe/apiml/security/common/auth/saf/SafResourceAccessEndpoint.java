/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.common.auth.saf;

import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class SafResourceAccessEndpoint implements SafResourceAccessVerifying {

    private static final String URL_VARIABLE_SUFFIX = "/{userId}/{class}/{entity}/{level}";

    @Value("${apiml.security.safEndpoint.url:'http://localhost:8542/saf-auth'}")
    private String endpointUrl;

    private final RestTemplate restTemplate;

    @Override
    public boolean hasSafResourceAccess(Authentication authentication, String resourceClass, String resourceName, String accessLevel) {
        Response response = restTemplate.getForObject(endpointUrl + URL_VARIABLE_SUFFIX, Response.class,
            authentication.getName(), resourceClass, resourceName, accessLevel);
        return response != null && !response.isError() && response.isAuthorized();
    }

    @Data
    @AllArgsConstructor
    public static class Response {

        private boolean authorized;
        private boolean error;
        private String message;

    }

}
