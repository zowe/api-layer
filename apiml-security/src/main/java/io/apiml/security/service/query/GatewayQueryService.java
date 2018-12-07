/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package io.apiml.security.service.query;

import io.apiml.security.gateway.query.QueryResponse;
import io.apiml.security.service.authentication.ApimlAuthentication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class GatewayQueryService {
    private final String queryUrl;
    private final RestTemplate restTemplate;

    public GatewayQueryService(String queryUrl) {
        this.queryUrl = queryUrl;
        this.restTemplate = new RestTemplate();
        this.restTemplate.setErrorHandler(new QueryServiceResponseErrorHandler());
    }

    @SuppressWarnings("unchecked")
    public ApimlAuthentication query(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity request = new HttpEntity(headers);
        ResponseEntity<QueryResponse> response = restTemplate
            .exchange(this.queryUrl, HttpMethod.GET, request, QueryResponse.class);
        ApimlAuthentication apimlAuthentication = new ApimlAuthentication(response.getBody().getUsername(), token);
        return apimlAuthentication;
    }
}
