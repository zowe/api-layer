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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class SafResourceAccessEndpoint implements SafResourceAccessVerifying {

    @SuppressWarnings("squid:S1075")
    private static final String PATH_VRIABLE_SUFFIX = "/{userId}/{class}/{entity}/{level}";

    @Value("${apiml.security.safEndpoint.url}")
    private String endpointUrl;

    private final RestTemplate restTemplate;

    @Override
    public boolean hasSafResourceAccess(Authentication authentication, String resourceClass, String resourceName, String accessLevel) {
        Response response = restTemplate.getForObject(endpointUrl + PATH_VRIABLE_SUFFIX, Response.class,
            authentication.getName(), resourceClass, resourceName, accessLevel);
        return !response.isError() && response.isAuthorized();
    }

    @Data
    @AllArgsConstructor
    public static class Response {

        private boolean authorized;
        private boolean error;
        private String message;

    }

}
