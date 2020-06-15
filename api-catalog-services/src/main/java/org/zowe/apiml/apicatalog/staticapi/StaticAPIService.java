/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.staticapi;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.apicatalog.discovery.DiscoveryConfigProperties;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class StaticAPIService {

    private static final String REFRESH_ENDPOINT = "discovery/api/v1/staticApi";

    @Value("${apiml.discovery.userid:eureka}")
    private String eurekaUserid;

    @Value("${apiml.discovery.password:password}")
    private String eurekaPassword;

    @Qualifier("restTemplateWithKeystore")
    private final RestTemplate restTemplate;

    private final DiscoveryConfigProperties discoveryConfigProperties;

    public StaticAPIResponse refresh() {
        String discoveryServiceUrl = getDiscoveryServiceUrl();
        HttpEntity<?> entity = getHttpEntity(discoveryServiceUrl);
        ResponseEntity<String> restResponse = restTemplate.exchange(discoveryServiceUrl,
            HttpMethod.POST, entity, String.class);
        return new StaticAPIResponse(restResponse.getStatusCode().value(), restResponse.getBody());
    }

    private HttpEntity<?> getHttpEntity(String discoveryServiceUrl) {
        boolean isHttp = discoveryServiceUrl.startsWith("http://");
        HttpHeaders httpHeaders = new HttpHeaders();
        if (isHttp) {
            String basicToken = "Basic " + Base64.getEncoder().encodeToString((eurekaUserid + ":" + eurekaPassword).getBytes());
            httpHeaders.add("Authorization", basicToken);
        }

        return new HttpEntity<>(null, httpHeaders);
    }

    private String getDiscoveryServiceUrl() {
        return discoveryConfigProperties.getLocations().replace("/eureka", "") + REFRESH_ENDPOINT;
    }
}
