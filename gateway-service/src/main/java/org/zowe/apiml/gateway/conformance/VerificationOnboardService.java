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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.gateway.security.service.TokenCreationService;

import java.util.*;

/**
 * Service class that offers methods for checking onboarding information and also checks availability metadata from
 * a provided serviceId.
 */
@Service
@RequiredArgsConstructor
public class VerificationOnboardService {

    private final DiscoveryClient discoveryClient;

    private final RestTemplate restTemplate;
    private final TokenCreationService tokenCreationService;

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
    public Optional<String> findSwaggerUrl(Map<String, String> metadata) {

        String swaggerKey = null;
        for (String key : metadata.keySet()) {
            if (key.contains("swaggerUrl")) {        // Find the correct key for swagger docs, can be both apiml.apiInfo.0.swaggerUrl or apiml.apiInfo.api-v1.swaggerUrl for example
                swaggerKey = key;
                break;
            }
        }
        if (swaggerKey == null) {
            return Optional.empty();
        }
        String swaggerUrl = metadata.get(swaggerKey);
        if (swaggerUrl != null) {
            return Optional.of(swaggerUrl);
        }
        return Optional.empty();
    }


    /**
     * Retrieves swagger from the url
     *
     * @param swaggerUrl URL to retrieve from
     * @return Swagger as string
     */
    public String getSwagger(String swaggerUrl) {
        HttpHeaders headersSSO = new HttpHeaders();
        headersSSO.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> requestSSO = new HttpEntity<>(headersSSO);
        return restTemplate.exchange(swaggerUrl, HttpMethod.GET, requestSSO, String.class).getBody();
    }

    /**
     * Checks if endpoints can be called and return documented responses
     *
     * @param getEndpoints              GET endpoints to check
     * @param passedAuthenticationToken Token used to call endpoints that support SSO
     * @return List of problems
     */
    public List<String> testEndpointsByCalling(Set<Endpoint> getEndpoints, String passedAuthenticationToken) {
        ArrayList<String> result = new ArrayList<>();

        String ssoCookie = getAuthenticationCookie(passedAuthenticationToken);

        HttpHeaders headersSSO = new HttpHeaders();
        headersSSO.setContentType(MediaType.APPLICATION_JSON);
        headersSSO.add("Cookie", "apimlAuthenticationToken=" + ssoCookie);
        HttpEntity<String> requestSSO = new HttpEntity<>(headersSSO);

        HttpHeaders headersNoSSO = new HttpHeaders();
        headersNoSSO.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestNoSSO = new HttpEntity<>(headersNoSSO);

        for (Endpoint endpoint : getEndpoints) {
            for (HttpMethod method : endpoint.getHttpMethods()) {
                checkEndpoint(endpoint, result, method, requestSSO, requestNoSSO);
            }
        }
        return result;
    }

    private void checkEndpoint(Endpoint endpoint, ArrayList<String> result, HttpMethod method, HttpEntity<String> requestSSO, HttpEntity<String> requestNoSSO) {
        if (method == HttpMethod.DELETE) {
            return;
        }

        String urlFromSwagger = endpoint.getUrl();
        // replaces parameters in {} in query
        String url = urlFromSwagger.replaceAll("\\{[^{}]*}", "dummy");

        ResponseEntity<String> responseWithSSO = getResponse(requestSSO, method, url);
        result.addAll(fromResponseReturnFoundProblems(responseWithSSO, endpoint, method, true));

        ResponseEntity<String> responseNoSSO = getResponse(requestNoSSO, method, url);
        result.addAll(fromResponseReturnFoundProblems(responseNoSSO, endpoint, method, false));
    }

    private ResponseEntity<String> getResponse(HttpEntity<String> request, HttpMethod method, String url) {
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, method, request, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            response = ResponseEntity.status(e.getRawStatusCode())
                .headers(e.getResponseHeaders())
                .body(e.getResponseBodyAsString());
        }
        return response;
    }

    private List<String> fromResponseReturnFoundProblems(ResponseEntity<String> response, Endpoint currentEndpoint, HttpMethod method, boolean attemptWithSSO) {
        ArrayList<String> result = new ArrayList<>();

        String responseBody = response.getBody();

        if (responseBody != null && response.getStatusCode() == HttpStatus.NOT_FOUND && responseBody.contains("ZWEAM104E")) {
            result.add("Documented endpoint at " + currentEndpoint.getUrl() + " could not be located, attempting to call it through gateway gives the ZWEAM104E error");
        }

        if (attemptWithSSO && responseBody != null && (response.getStatusCode() == HttpStatus.FORBIDDEN || response.getStatusCode() == HttpStatus.UNAUTHORIZED)) {
            result.add(method + " request to documented endpoint at " + currentEndpoint.getUrl() + " responded with status code " + response.getStatusCode().value() + ", despite being called with the SSO authorization");
        }

        if (!currentEndpoint.isResponseCodeForMethodDocumented(String.valueOf(response.getStatusCode().value()), method)) {
            result.add(method + " request to documented endpoint at " + currentEndpoint.getUrl() + " returns undocumented " + response.getStatusCode().value()
                + " status code, documented responses are:" + currentEndpoint.getValidResponses().get(method.toString()));
        }
        return result;
    }

    public static List<String> getProblemsWithEndpointUrls(AbstractSwaggerValidator swaggerParser) {
        return swaggerParser.getProblemsWithEndpointUrls();
    }


    private String getAuthenticationCookie(String passedAuthenticationToken) {
        if (passedAuthenticationToken.equals("dummy")) {
            return tokenCreationService.createJwtTokenWithoutCredentials("validate");
        }
        return passedAuthenticationToken;
    }

    public static boolean supportsSSO(Map<String, String> metadata) {
        if (metadata.containsKey(EurekaMetadataDefinition.AUTHENTICATION_SSO)) {
            return metadata.get(EurekaMetadataDefinition.AUTHENTICATION_SSO).equals("true");
        }
        return false;
    }
}


