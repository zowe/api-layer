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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.apicatalog.discovery.DiscoveryConfigProperties;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Getter
public class StaticAPIService {

    private static final String REFRESH_ENDPOINT = "discovery/api/v1/staticApi";

    @Value("${apiml.discovery.userid:eureka}")
    private String eurekaUserid;

    @Value("${apiml.discovery.password:password}")
    private String eurekaPassword;

    @Qualifier("restTemplateWithKeystore")
    private final RestTemplate restTemplate;

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    private final DiscoveryConfigProperties discoveryConfigProperties;

    public StaticAPIResponse refresh() {
        List<String> discoveryServiceUrls = getDiscoveryServiceUrls(REFRESH_ENDPOINT);
        for (int i = 0; i < discoveryServiceUrls.size(); i++) {

            String discoveryServiceUrl = discoveryServiceUrls.get(i);
            HttpEntity<?> entity = getHttpEntity(discoveryServiceUrl, false);

            try {
                ResponseEntity<String> response = restTemplate.exchange(discoveryServiceUrl, HttpMethod.POST, entity, String.class);

                // Return response if successful response or if none have been successful and this is the last URL to try
                if (response.getStatusCode().is2xxSuccessful() || i == discoveryServiceUrls.size() - 1) {
                    return new StaticAPIResponse(response.getStatusCode().value(), response.getBody());
                }

            } catch (Exception e) {
                log.debug("Error refreshing static APIs from {}, error message: {}", discoveryServiceUrl, e.getMessage());
            }
        }

        return new StaticAPIResponse(500, "Error making static API refresh request to the Discovery Service");
    }

    protected HttpEntity<?> getHttpEntity(String discoveryServiceUrl, boolean isProtected) {
        boolean isHttp = discoveryServiceUrl.startsWith("http://");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Accept", "application/json");
        String token;
        if (isHttp && !isAttlsEnabled) {
            token = "Basic " + Base64.getEncoder().encodeToString((eurekaUserid + ":" + eurekaPassword).getBytes());
            httpHeaders.add("Authorization", token);
        } else if (isProtected) {
            token = "apimlAuthenticationToken=" + SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            httpHeaders.add("Cookie", token);
        }

        return new HttpEntity<>(null, httpHeaders);
    }

    protected List<String> getDiscoveryServiceUrls(String path) {
        String[] discoveryServiceLocations = discoveryConfigProperties.getLocations();

        List<String> discoveryServiceUrls = new ArrayList<>();
        for (String location : discoveryServiceLocations) {
            discoveryServiceUrls.add(location.replace("/eureka", "") + path);
        }
        return discoveryServiceUrls;
    }
}
