/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.gateway.filters.post;

import com.broadcom.apiml.library.service.security.service.gateway.services.routing.RoutedService;
import com.broadcom.apiml.library.service.security.service.gateway.services.routing.RoutedServices;
import com.broadcom.apiml.library.service.security.service.gateway.services.routing.RoutedServicesUser;
import com.broadcom.apiml.library.service.security.service.gateway.filters.pre.FilterUtils;
import com.broadcom.apiml.library.service.security.test.integration.product.constants.CoreService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import javax.validation.UnexpectedTypeException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.broadcom.apiml.library.service.security.test.integration.product.constants.ApimConstants.API_DOC_NORMALISED;
import static com.netflix.zuul.context.RequestContext.getCurrentContext;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_URI_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * Transform api-doc endpoints to ones that are relative to the gateway, not the service instance
 *
 * @author Dave King
 */
public class TransformApiDocEndpointsFilter extends ZuulFilter implements RoutedServicesUser {

    private static final String SEPARATOR = "/";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TransformApiDocEndpointsFilter.class);
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
     * @param context     request context
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
        if ((requestPath.contains("/api-doc") || requestPath.contains(CoreService.API_CATALOG.getServiceId() + "/apidoc/")) && !requestPath.contains("/api-doc/enabled")
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
     *
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

        Map<String, Path> updatedShortPaths = new HashMap<>();
        Map<String, Path> updatedLongPaths = new HashMap<>();
        String serviceId = (String) context.get(SERVICE_ID_KEY);
        String requestUri = (String) context.get(REQUEST_URI_KEY);

        // if this is an API Catalog swagger request, extract the swagger owner service id from the request URI
        String apiDocEndPoint = "/" + CoreService.API_CATALOG.getServiceId() + "/apidoc/";
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
            String swaggerLocationHeaderLink = "[Swagger/OpenAPI JSON Document]";
            String swaggerLink = "\n\n" + swaggerLocationHeaderLink + "(" + context.getRequest().getRequestURL() + ")";
            // do not add link if it already exists
            if (!swagger.getInfo().getDescription().contains(swaggerLocationHeaderLink)) {
                swagger.getInfo().setDescription(description + swaggerLink);
            }
        }

        // Update all paths to gateway format
        Set<String> prefixes = new HashSet<>();
        if (swagger.getPaths() != null && !swagger.getPaths().isEmpty()) {

            // Check each path
            String finalServiceId = serviceId;
            swagger.getPaths().forEach((originalEndpoint, path) -> {

                // Logging
                log.trace("Swagger Service Id: " + finalServiceId);
                log.trace("Original Endpoint: " + originalEndpoint);
                log.trace("Base Path: " + swagger.getBasePath());

                // Retrieve route which matches endpoint
                String endPoint = swagger.getBasePath().equals(SEPARATOR) ? originalEndpoint : swagger.getBasePath() + originalEndpoint;
                RoutedService route = getRouteServiceForEndpoint(endPoint, finalServiceId);

                String updatedShortEndPoint = null;
                String updatedLongEndPoint = null;
                if (route != null) {
                    prefixes.add(route.getGatewayUrl());
                    updatedShortEndPoint = endPoint.replace(route.getServiceUrl(), "");
                    updatedLongEndPoint = SEPARATOR + route.getGatewayUrl() + SEPARATOR + finalServiceId + updatedShortEndPoint;
                }
                log.trace("Final Endpoint: " + updatedLongEndPoint);

                // If endpoint not converted, then use original
                if (updatedLongEndPoint != null) {
                    updatedShortPaths.put(updatedShortEndPoint, path);
                    updatedLongPaths.put(updatedLongEndPoint, path);
                } else {
                    log.debug("Could not transform endpoint: " + originalEndpoint + ", original used");
                }
            });
        }

        // update basePath and the original swagger object with the new paths
        if (prefixes.size() == 1) {
            swagger.setBasePath(SEPARATOR + prefixes.iterator().next() + SEPARATOR + serviceId);
            swagger.setPaths(updatedShortPaths);
        } else {
            swagger.setBasePath("");
            swagger.setPaths(updatedLongPaths);
        }

        // update scheme and host
        String scheme = context.getRequest().getScheme();
        swagger.setSchemes(Collections.singletonList(Scheme.forValue(scheme)));
        swagger.setHost(context.getRequest().getHeader(HttpHeaders.HOST.toLowerCase()));

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
     * Get the transformation routes for the endpoint
     *
     * @param endPoint  the endpoint
     * @param serviceId the service id
     * @return modified content
     */
    private RoutedService getRouteServiceForEndpoint(String endPoint, String serviceId) {
        RoutedServices routedServices = routedServicesMap.get(serviceId);
        if (routedServices == null) {
            return null;
        } else {
            return routedServices.findGatewayUrlThatMatchesServiceUrl(endPoint, true);
        }
    }
}
