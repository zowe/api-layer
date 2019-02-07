package com.ca.mfaas.apicatalog.swagger;

import com.ca.mfaas.apicatalog.exceptions.ApiDocTransformationException;
import com.ca.mfaas.apicatalog.metadata.EurekaMetadataParser;
import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.gateway.services.routing.RoutedService;
import com.ca.mfaas.gateway.services.routing.RoutedServices;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import lombok.extern.slf4j.Slf4j;

import javax.validation.UnexpectedTypeException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class TransformApiDocService {
    private static final String SEPARATOR = "/";

    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();

    public String transformApiDoc(String serviceId, String apiDoc) {
        Swagger swagger;

        try {
            swagger = Json.mapper().readValue(apiDoc, Swagger.class);
        } catch (IOException e) {
            log.error("Could not convert response body to a Swagger object.", e);
            throw new UnexpectedTypeException("Response is not a Swagger type object.");
        }

        //updatePaths(serviceId, swagger);

        try {
            return Json.mapper().writeValueAsString(swagger);
        } catch (JsonProcessingException e) {
            log.error("Could not convert Swagger to JSON", e);
            throw new ApiDocTransformationException("Could not convert Swagger to JSON");
        }
    }

    private void updatePaths(String serviceId, Swagger swagger) {
        Map<String, Path> updatedShortPaths = new HashMap<>();
        Map<String, Path> updatedLongPaths = new HashMap<>();
        Set<String> prefixes = new HashSet<>();

        metadataParser

        if (swagger.getPaths() != null && !swagger.getPaths().isEmpty()) {
            swagger.getPaths().forEach((originalEndpoint, path) -> {

                // Logging
                log.trace("Swagger Service Id: " + serviceId);
                log.trace("Original Endpoint: " + originalEndpoint);
                log.trace("Base Path: " + swagger.getBasePath());

                // Retrieve route which matches endpoint
                String endPoint = swagger.getBasePath().equals(SEPARATOR) ? originalEndpoint : swagger.getBasePath() + originalEndpoint;
               / RoutedService route = getRouteServiceForEndpoint(endPoint, serviceId);

                String updatedShortEndPoint = null;
                String updatedLongEndPoint = null;
                if (route != null) {
                    prefixes.add(route.getGatewayUrl());
                    updatedShortEndPoint = endPoint.replace(route.getServiceUrl(), "");
                    updatedLongEndPoint = SEPARATOR + route.getGatewayUrl() + SEPARATOR + serviceId + updatedShortEndPoint;
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
    }

}
