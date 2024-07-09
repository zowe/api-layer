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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.product.constants.CoreService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service class that offers methods for checking onboarding information and also checks availability metadata from
 * a provided serviceId.
 */
@Service
@RequiredArgsConstructor
public class VerificationOnboardService {

    private final DiscoveryClient discoveryClient;
    @Qualifier("restTemplateWithoutKeystore")
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
     * @param endpoints                 endpoints to check
     * @param passedAuthenticationToken Token used to call endpoints that support SSO
     * @return List of problems
     */
    public List<String> testEndpointsByCalling(Set<Endpoint> endpoints, String passedAuthenticationToken) {
        ArrayList<String> result = new ArrayList<>(checkEndpointsNoSSO(endpoints));
        try {
            result.addAll(checkEndpointsWithSSO(endpoints, passedAuthenticationToken));
        } catch (ValidationException e) {
            result.add(e.getMessage());
        }

        return result;
    }

    private List<String> checkEndpointsWithSSO(Set<Endpoint> endpoints, String passedAuthenticationToken) {
        ArrayList<String> result = new ArrayList<>();

        String ssoCookie = getAuthenticationCookie(passedAuthenticationToken);

        HttpHeaders headersSSO = new HttpHeaders();
        headersSSO.setContentType(MediaType.APPLICATION_JSON);
        headersSSO.add("Cookie", "apimlAuthenticationToken=" + ssoCookie);
        HttpEntity<String> requestSSO = new HttpEntity<>(headersSSO);

        for (Endpoint endpoint : endpoints) {
            for (HttpMethod method : endpoint.getHttpMethods()) {
                checkEndpoint(endpoint, result, method, requestSSO, true);
            }
        }
        return result;
    }

    private List<String> checkEndpointsNoSSO(Set<Endpoint> endpoints) {
        ArrayList<String> result = new ArrayList<>();

        HttpHeaders headersNoSSO = new HttpHeaders();
        headersNoSSO.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestNoSSO = new HttpEntity<>(headersNoSSO);

        for (Endpoint endpoint : endpoints) {
            for (HttpMethod method : endpoint.getHttpMethods()) {
                checkEndpoint(endpoint, result, method, requestNoSSO, false);
            }
        }
        return result;
    }

    private void checkEndpoint(Endpoint endpoint, ArrayList<String> result, HttpMethod method, HttpEntity<String> request, boolean attemptWithSSO) {
        if (method == HttpMethod.DELETE) {
            return;
        }

        String urlFromSwagger = endpoint.getUrl();
        // replaces parameters in {} in query, so it can be called
        // Example: transforms https://localhost:10010/discoverableclient/api/v1/pets/{id} to https://localhost:10010/discoverableclient/api/v1/pets/dummy
        String url = urlFromSwagger.replaceAll("\\{[^{}]*}", "dummy");

        ResponseEntity<String> responseWithSSO = getResponse(request, method, url);
        result.addAll(fromResponseReturnFoundProblems(responseWithSSO, endpoint, method, attemptWithSSO));
    }

    private ResponseEntity<String> getResponse(HttpEntity<String> request, HttpMethod method, String url) {
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, method, request, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            response = ResponseEntity.status(e.getStatusCode())
                .headers(e.getResponseHeaders())
                .body(e.getResponseBodyAsString());
        } catch (Exception ex) {
            response = ResponseEntity.status(HttpStatusCode.valueOf(500)).body(ex.getMessage());
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
        String errorMsg = "Error retrieving ZAAS connection details";
        // FIXME This keeps the current behaviour
        if (passedAuthenticationToken.equals("dummy")) {
            URI uri = discoveryClient.getServices().stream()
                .flatMap(service -> discoveryClient.getInstances(service).stream())
                .filter(service -> CoreService.ZAAS.getServiceId().equalsIgnoreCase(service.getServiceId()))
                .findFirst()
                .map(ServiceInstance::getUri)
                .orElseThrow(() -> new ValidationException(errorMsg, ValidateAPIController.NO_METADATA_KEY));

            String zaasAuthValidateUri = String.format("%s://%s:%d%s", uri.getScheme() == null ? "https" : uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath() + "/zaas/validate/auth");
            ResponseEntity<String> validationResponse = restTemplate.exchange(zaasAuthValidateUri, HttpMethod.GET, null, String.class);
            if (validationResponse.getStatusCode() == HttpStatus.CONFLICT) {
                throw new ValidationException(validationResponse.getBody(), ValidateAPIController.NON_CONFORMANT_KEY);
            }

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


