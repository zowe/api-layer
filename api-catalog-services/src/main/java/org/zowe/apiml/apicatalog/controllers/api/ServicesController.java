/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.apicatalog.exceptions.ContainerStatusRetrievalThrowable;
import org.zowe.apiml.apicatalog.instance.InstanceInitializeService;
import org.zowe.apiml.apicatalog.model.APIContainer;
import org.zowe.apiml.apicatalog.model.APIService;
import org.zowe.apiml.apicatalog.services.cached.CachedApiDocService;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Main API for handling requests from the API Catalog UI, routed through the gateway
 */
@Slf4j
@RestController
@RequestMapping("/")
@Tag(name = "API Catalog")
@RequiredArgsConstructor
public class ServicesController {

    private final InstanceInitializeService instanceInitializeService;
    private final CachedApiDocService cachedApiDocService;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    /**
     * Get all containers
     *
     * @return a list of all containers
     */
    @GetMapping(value = "/containers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lists catalog dashboard tiles",
        description = "Returns a list of tiles including status and tile description",
        security = {
            @SecurityRequirement(name = "BasicAuthorization"), @SecurityRequirement(name = "CookieAuth")
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "204", description = "No service available"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "URI not found"),
        @ApiResponse(responseCode = "500", description = "An unexpected condition occurred")
    })
    public ResponseEntity<List<APIContainer>> getAllAPIContainers() throws ContainerStatusRetrievalThrowable {
        try {
            Iterable<APIContainer> allContainers = instanceInitializeService.getAllContainers();
            List<APIContainer> apiContainers = toList(allContainers);
            if (apiContainers == null || apiContainers.isEmpty()) {
                return new ResponseEntity<>(apiContainers, HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(apiContainers, HttpStatus.OK);
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.apicatalog.containerCouldNotBeRetrieved", e.getMessage());
            throw new ContainerStatusRetrievalThrowable(e);
        }
    }

    /**
     * Get all containers (and included services)
     *
     * @return a containers by id
     */
    @GetMapping(value = "/containers/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves a specific dashboard tile information",
        description = "Returns information for a specific tile {id} including status and tile description",
        security = {
            @SecurityRequirement(name = "BasicAuthorization"), @SecurityRequirement(name = "CookieAuth")
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "URI not found"),
        @ApiResponse(responseCode = "500", description = "An unexpected condition occurred")
    })
    public ResponseEntity<List<APIContainer>> getAPIContainerById(@PathVariable(value = "id") String id) throws ContainerStatusRetrievalThrowable {
        try {
            List<APIContainer> apiContainers = new ArrayList<>();
            APIContainer containerById = instanceInitializeService.getContainerById(id);
            if (containerById != null) {
                apiContainers.add(containerById);
            }
            if (!apiContainers.isEmpty()) {
                apiContainers.forEach(apiContainer -> {
                    // add API Doc to the services to improve UI performance
                    setApiDocToService(apiContainer);
                });
            }
            return new ResponseEntity<>(apiContainers, HttpStatus.OK);
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.apicatalog.containerCouldNotBeRetrieved", e.getMessage());
            throw new ContainerStatusRetrievalThrowable(e);
        }
    }

    /**
     * Get a specific service by id
     *
     * @return a service by id
     */
    @GetMapping(value = "/services/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves a specific service information",
        description = "Returns information for a specific service {id} including status and service description",
        security = {
            @SecurityRequirement(name = "BasicAuthorization"), @SecurityRequirement(name = "CookieAuth")
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "204", description = "No service available"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "URI not found"),
        @ApiResponse(responseCode = "500", description = "An unexpected condition occurred")
    })
    public ResponseEntity<APIService> getAPIServicesById(@PathVariable(value = "id") String id) throws ContainerStatusRetrievalThrowable {
        try {
            var service = instanceInitializeService.getService(id);
            if (service == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            log.debug("Getting service by id {}", id);
            String apiDoc = cachedApiDocService.getDefaultApiDocForService(id);
            log.debug("Getting service: {} with status {}", service.getServiceId(), service.getStatus());

            if (apiDoc != null) {
                log.debug("API doc was retrieved");
                service.setApiDoc(apiDoc);
                List<String> apiVersions = cachedApiDocService.getApiVersionsForService(id);
                service.setApiVersions(apiVersions);
                log.debug("Got API versions: {}", apiVersions != null ? apiVersions.size() : 0);
                String defaultApiVersion = cachedApiDocService.getDefaultApiVersionForService(id);
                log.debug("Default API version: {}", defaultApiVersion);
                service.setDefaultApiVersion(defaultApiVersion);
            } else {
                log.debug("No API doc was retrieved for service with id {}", id);
            }
            return new ResponseEntity<>(service, HttpStatus.OK);
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.apicatalog.serviceCouldNotBeRetrieved", e.getMessage());
            throw new ContainerStatusRetrievalThrowable(e);
        }
    }

    private void setApiDocToService(APIContainer apiContainer) {
        apiContainer.getServices().forEach(apiService -> {
            // try the get the Api Doc for this service, if it fails for any reason then do not change the existing value
            // it may or may not be null
            String serviceId = apiService.getServiceId();
            try {
                String apiDoc = cachedApiDocService.getDefaultApiDocForService(serviceId);
                if (apiDoc != null) {
                    apiService.setApiDoc(apiDoc);
                }

                List<String> apiVersions = cachedApiDocService.getApiVersionsForService(serviceId);
                apiService.setApiVersions(apiVersions);

                String defaultApiVersion = cachedApiDocService.getDefaultApiVersionForService(serviceId);
                apiService.setDefaultApiVersion(defaultApiVersion);
            } catch (Exception e) {
                log.debug("An error occurred when trying to fetch ApiDoc for service: {}, processing can continue but this service will not be able to display any Api Documentation.\nError:", serviceId, e);
                apiService.setApiDocErrorMessage("Failed to fetch API documentation: " + e.getMessage());
            }
        });
    }

    /**
     * Convert an iterable to a list
     *
     * @param iterable the collection to convert
     * @param <T>      the type of the collection
     * @return a list
     */
    private <T> List<T> toList(final Iterable<T> iterable) {
        if (iterable == null) {
            return Collections.emptyList();
        }
        return StreamSupport.stream(iterable.spliterator(), false)
            .toList();
    }
}
