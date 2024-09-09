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
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.zowe.apiml.apicatalog.discovery.DiscoveryConfigProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaticAPIService {

    private static final String REFRESH_ENDPOINT = "discovery/api/v1/staticApi";

    @Value("${apiml.discovery.userid:eureka}")
    private String eurekaUserid;

    @Value("${apiml.discovery.password:password}")
    private String eurekaPassword;

    @Qualifier("secureHttpClientWithKeystore")
    private final CloseableHttpClient httpClient;

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    private final DiscoveryConfigProperties discoveryConfigProperties;

    public StaticAPIResponse refresh() {
        List<String> discoveryServiceUrls = getDiscoveryServiceUrls();
        for (int i = 0; i < discoveryServiceUrls.size(); i++) {

            String discoveryServiceUrl = discoveryServiceUrls.get(i);

            try {
                HttpPost post = getHttpRequest(discoveryServiceUrl);
                var staticApiResponse = httpClient.execute(post, response -> {
                    final HttpEntity responseEntity = response.getEntity();
                    String responseBody = "";
                    if (responseEntity != null) {
                        responseBody = new BufferedReader(new InputStreamReader(responseEntity.getContent())).lines().collect(Collectors.joining("\n"));
                    }
                    return new StaticAPIResponse(response.getCode(), responseBody);
                });

                // Return response if successful or if none have been successful and this is the last URL to try
                if (isSuccessful(staticApiResponse) || i == discoveryServiceUrls.size() - 1) {
                    return staticApiResponse;
                }
            } catch (IOException e) {
                log.debug("Error refreshing static APIs from {}, error message: {}", discoveryServiceUrl, e.getMessage());
            }
        }

        return new StaticAPIResponse(500, "Error making static API refresh request to the Discovery Service");
    }

    private boolean isSuccessful(StaticAPIResponse response) {
        return HttpStatus.valueOf(response.getStatusCode()).is2xxSuccessful();
    }

    private HttpPost getHttpRequest(String discoveryServiceUrl) {
        boolean isHttp = discoveryServiceUrl.startsWith("http://");
        HttpPost post = new HttpPost(discoveryServiceUrl);
        post.addHeader("Accept", "application/json");
        if (isHttp && !isAttlsEnabled) {
            String basicToken = "Basic " + Base64.getEncoder().encodeToString((eurekaUserid + ":" + eurekaPassword).getBytes());
            post.addHeader("Authorization", basicToken);
        }
        return post;
    }

    private List<String> getDiscoveryServiceUrls() {
        String[] discoveryServiceLocations = discoveryConfigProperties.getLocations();

        List<String> discoveryServiceUrls = new ArrayList<>();
        for (String location : discoveryServiceLocations) {
            discoveryServiceUrls.add(location.replace("/eureka", "") + REFRESH_ENDPOINT);
        }
        return discoveryServiceUrls;
    }
}
