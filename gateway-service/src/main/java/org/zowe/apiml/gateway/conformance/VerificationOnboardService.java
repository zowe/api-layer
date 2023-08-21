/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class that offers methods for checking onboarding information and also checks availability metadata from
 * a provided serviceId.
 */
@Service
@RequiredArgsConstructor
public class VerificationOnboardService {

    private final DiscoveryClient discoveryClient;

    private final RestTemplate restTemplate;


    /**
     * Accepts serviceId and checks if the service is onboarded to the API Mediation Layer
     *
     * @param serviceId serviceId to check
     * @return true if the service is known by Eureka otherwise false.
     */
    public boolean checkOnboarding(String serviceId) {

        List<String> serviceLists = discoveryClient.getServices();

        return serviceLists.contains(serviceId);

    }


    /**
     * Accepts metadata and retrieves the Swagger url if it exists
     *
     * @param metadata to grab swagger from
     * @return SwaggerUrl when able, empty string otherwise
     */
    public String findSwaggerUrl(Map<String, String> metadata) {

        String swaggerKey = null;
        for (String key : metadata.keySet()) {
            if (key.contains("swaggerUrl")) {        // Find the correct key for swagger docs, can be both apiml.apiInfo.0.swaggerUrl or apiml.apiInfo.api-v1.swaggerUrl for example
                swaggerKey = key;
                break;
            }
        }
        if (swaggerKey == null) {
            return "";
        }
        String swaggerUrl = metadata.get(swaggerKey);
        if (swaggerUrl != null) {
            return swaggerUrl;
        }
        return "";
    }


    /**
     * Retrieves swagger from the url
     *
     * @param swaggerUrl URL to retrieve from
     * @return Swagger as string
     */
    public String getSwagger(String swaggerUrl) {
        String response;
        response = restTemplate.getForEntity(swaggerUrl, String.class).getBody();
        return response;
    }

    /**
     * Checks if endpoints can be called and return documented responses
     *
     * @param getEndpoints GET endpoints to check
     * @return List of problems
     */
    public List<String> testGetEndpoints(Set<Endpoint> getEndpoints) {
        ArrayList<String> result = new ArrayList<>();
        boolean gotResponseDifferentFrom404 = false;

        for (Endpoint endpoint : getEndpoints) {
            String urlFromSwagger = endpoint.getUrl();
            // replaces parameters in {} in query
            String url = urlFromSwagger.replaceAll("\\{[^{}]*}", "dummy");

            ResponseEntity<String> response;
            try {
                response = restTemplate.getForEntity(url, String.class);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                response = ResponseEntity.status(e.getRawStatusCode()).headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsString());
            }

            if (response.getStatusCode() != HttpStatus.NOT_FOUND) {
                gotResponseDifferentFrom404 = true;
            }
            if (!(endpoint.getValidResponses().get("GET").contains(String.valueOf(response.getStatusCode().value())) || endpoint.getValidResponses().get("GET").contains("default"))) {
                result.add("Calling endpoint at " + endpoint.getUrl() + " gives undocumented " + response.getStatusCode().value()
                    + " status code, documented responses are:" + endpoint.getValidResponses().get("GET"));
            }
        }
        if (!gotResponseDifferentFrom404) {
            result.add("Could not verify if API can be called through gateway, attempting to reach all of the following " +
                "documented GET endpoints gives only 404 response: " + getEndpoints.stream().map(Endpoint::getUrl).collect(Collectors.toSet()));
        }
        return result;
    }

    public List<String> getProblemsWithEndpointUrls(AbstractSwaggerValidator swaggerParser) {
        return swaggerParser.getProblemsWithEndpointUrls();
    }
}
