/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.filters.post;

import com.ca.mfaas.gateway.filters.pre.FilterUtils;
import com.ca.mfaas.gateway.services.routing.RoutedService;
import com.ca.mfaas.gateway.services.routing.RoutedServices;
import com.ca.mfaas.gateway.services.routing.RoutedServicesUser;
import com.ca.mfaas.product.family.ProductFamilyType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.HttpStatus;

import javax.validation.UnexpectedTypeException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ca.mfaas.product.constants.ApimConstants.API_DOC_NORMALISED;
import static com.netflix.zuul.context.RequestContext.getCurrentContext;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * Transform api-doc endpoints to ones that are relative to the gateway, not the service instance
 *
 * @author Dave King
 */
@Slf4j
public class TransformApiDocEndpointsFilter extends ZuulFilter implements RoutedServicesUser {

    private final Map<String, RoutedServices> routedServicesMap = new HashMap<>();

    /**
     * Process this response
     *
     * @return always null
     */
    @Override
    public Object run() {
        // Modify paths and return now body content
        String convertedBody = null;
        try {
            convertedBody = convertPaths();
        } catch (JsonProcessingException e) {
            log.error("Could not convert updated API Doc contents to JSON object", e);
        }
        getCurrentContext().setResponseBody(convertedBody);

        return null;
    }

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext context = getCurrentContext();
        final String serviceId = (String) context.get(SERVICE_ID_KEY);
        final String requestPath = FilterUtils.addFirstSlash((String) context.get(REQUEST_URI_KEY));
        return isRequestThatCanBeProcessed(serviceId, requestPath, context);
    }

    public void addRoutedServices(String serviceId, RoutedServices routedServices) {
        routedServicesMap.put(serviceId, routedServices);
    }

    /**
     * Is this response processable
     *
     * @param serviceId   the service id
     * @param requestPath the request path
     * @param context request context
     * @return true if this can be processed
     */
    private boolean isRequestThatCanBeProcessed(String serviceId, String requestPath, RequestContext context) {
        if (serviceId == null || context.getThrowable() != null) {
            return false;
        }
        if (context.getResponse().getStatus() != HttpStatus.OK.value()) {
            return false;
        }

        // Transform a request for a service /api-doc or apicatalog/apidoc/**
        // The catalog request is transformed here rather than when first retrieved due to the dependencies on routes
        if ((requestPath.contains("/api-doc") || requestPath.contains(ProductFamilyType.API_CATALOG.getServiceId() + "/apidoc/")) && !requestPath.contains("/api-doc/enabled")
            && (context.getResponseDataStream() != null || context.getResponseBody() != null)) {
            List<Pair<String, String>> zuulResponseHeaders = context.getZuulResponseHeaders();
            if (zuulResponseHeaders != null) {
                boolean shouldNormalise = true;
                List<Pair<String, String>> filteredResponseHeaders = new ArrayList<>();
                for (Pair<String, String> it : zuulResponseHeaders) {
                    if (it.first().contains(API_DOC_NORMALISED)) {
                        if (Boolean.valueOf(it.second())) {
                            log.debug("Api Doc is already normalised for: " + requestPath + ", transformation not required.");
                            shouldNormalise = false;
                        }
                        break;
                    } else {
                        filteredResponseHeaders.add(it);
                    }
                }
                // remove "Api-Doc-Normalised" from response
                context.put("zuulResponseHeaders", filteredResponseHeaders);
                return shouldNormalise;
            }
            log.debug("Normalising endpoints for: " + requestPath);
            return true;
        }
        return false;
    }


    /**
     * Retrieve the body as a String from the data stream
     * @param responseStream the data stream
     * @return a string
     * @throws IOException body could not be retrieved
     */
    private String getBodyFromStream(InputStream responseStream) throws IOException {
        BufferedInputStream inputStream = new BufferedInputStream(responseStream);
        inputStream.mark(Integer.MAX_VALUE);
        inputStream.reset();
        return IOUtils.toString(inputStream, "UTF-8");
    }


    /**
     * Convert paths to gateway friendly endpoints
     *
     * @return modified content
     */
    private String convertPaths() throws JsonProcessingException {
        Swagger swagger;
        RequestContext context = getCurrentContext();
        String body = context.getResponseBody();
        if (body == null) {
            try {
                body = getBodyFromStream(context.getResponseDataStream());
            } catch (IOException e) {
                log.error("Error reading body for Api Doc", e);
                return null;
            }
        }

        if (body.contains("apimlNoTransformation")) {
            return body;
        }

        try {
            // Convert body to Swagger
            swagger = Json.mapper().readValue(body, Swagger.class);
        } catch (IOException e) {
            log.error("Could not convert response body to a Swagger object.", e);
            // Show original response in logs
            log.error("RESPONSE: " + context.getResponseStatusCode() + "\n" + body);
            throw new UnexpectedTypeException("Response is not a Swagger type object. Http Response: " + context.getResponseStatusCode());
        }

        Map<String, Path> updatedPaths = new HashMap<>();
        String serviceId = (String) context.get(SERVICE_ID_KEY);
        String requestUri = (String) context.get(REQUEST_URI_KEY);

        // if this is an API Catalog swagger request, extract the swagger owner service id from the request URI
        String apiDocEndPoint = "/" + ProductFamilyType.API_CATALOG.getServiceId() + "/apidoc/";
        if (requestUri.startsWith(apiDocEndPoint)) {
            requestUri = requestUri.replace(apiDocEndPoint, "");
            requestUri = requestUri.substring(0, requestUri.indexOf('/'));
            // the request service id will not be the service id of the Swagger owner, reset it
            if (!requestUri.equals(serviceId)) {
                log.warn("Modified serviceId from: " + serviceId + " to: " + requestUri);
                serviceId = requestUri;
            }
        }

        // Add link to swagger to the description:
        String description = swagger.getInfo().getDescription();
        if (context.getRequest() != null) {
            String swaggerLink = "\n\n[Swagger/OpenAPI JSON Document](" + context.getRequest().getRequestURL() + ")";
            swagger.getInfo().setDescription(description + swaggerLink);
        }

        // Update all paths to gateway format
        if (swagger.getPaths() != null && !swagger.getPaths().isEmpty()) {

            // Check each path
            String finalServiceId = serviceId;
            swagger.getPaths().forEach((originalEndpoint, path) -> {

                // Logging
                log.trace("Swagger Service Id: " + finalServiceId);
                log.trace("Original Endpoint: " + originalEndpoint);
                log.trace("Base Path: " + swagger.getBasePath());

                // Retrieve route which matches endpoint
                String updatedEndPoint = getGatewayURLForEndPoint(swagger.getBasePath() + originalEndpoint, finalServiceId);
                log.trace("Final Endpoint: " + updatedEndPoint);
                // If endpoint not converted, then use original
                if (updatedEndPoint != null) {
                    updatedPaths.put(updatedEndPoint, path);
                } else {
                    log.debug("Could not transform endpoint: " + originalEndpoint + ", original used");
                }
            });

            // update the original swagger object with the new paths
            swagger.setPaths(updatedPaths);
        }

        // update host and base path
        swagger.setBasePath("");
        String baseHost = null;
        try {
            baseHost = new URIBuilder()
                .setHost(context.getZuulRequestHeaders().get(X_FORWARDED_HOST_HEADER.toLowerCase()))
                .setScheme(context.getZuulRequestHeaders().get(X_FORWARDED_PROTO_HEADER.toLowerCase())).build().toString();
        } catch (URISyntaxException e) {
            log.warn("An error occurred when setting host in the Api Doc, setting it to fallback value", e);
            baseHost = context.getZuulRequestHeaders().get(X_FORWARDED_HOST_HEADER.toLowerCase());
        }
        swagger.setHost(baseHost);

        // Convert to JSON and set content body
        try {
            body = Json.mapper().writeValueAsString(swagger);
        } catch (JsonProcessingException e) {
            log.error("Could not convert Swagger to JSON", e);
            throw e;
        }
        return body;
    }

    /**
     * Get the base path for this Api Version
     *
     * @return the base path
     * @param endPoint the REST endpoint (relative to service)
     * @param serviceId the service id
     */
    private String getGatewayURLForEndPoint(String endPoint, String serviceId) {
        String updatedEndPoint = null;
        String basePath = null;

        // Get the transformation routes for this service
        RoutedServices routedServices = routedServicesMap.get(serviceId);
        if (routedServices != null) {
            RoutedService route = routedServices.findGatewayUrlThatMatchesServiceUrl(endPoint, true);
            if (route != null) {
                String separator = "/";
                basePath = separator + route.getGatewayUrl();
                // does the service have a servlet context or is the base = "/"
                if (endPoint.startsWith("//")) {
                    updatedEndPoint = separator + serviceId + endPoint.replace("//", "/");
                }
                updatedEndPoint = separator + serviceId + endPoint.replace(route.getServiceUrl(), "");

                log.trace("Updated Endpoint: " + updatedEndPoint);

                // remove any prefixes where the base path may also contain the service Id
                String duplicatePrefix = "/" + serviceId + "/" + serviceId;
                if (updatedEndPoint.startsWith(duplicatePrefix)) {
                    updatedEndPoint = updatedEndPoint.replace(duplicatePrefix, "/" + serviceId);
                    log.debug("Duplicate Modified Endpoint: " + updatedEndPoint);
                }
            } else {
                // if no route found then do not update endpoint, use original
                return null;
            }
        }
        return basePath + updatedEndPoint;
    }
}
