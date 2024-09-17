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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.apicatalog.exceptions.ContainerStatusRetrievalThrowable;
import org.zowe.apiml.apicatalog.model.APIContainer;
import org.zowe.apiml.apicatalog.services.cached.CachedApiDocService;
import org.zowe.apiml.apicatalog.services.cached.CachedProductFamilyService;
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
public class ApiCatalogController {

    private final CachedProductFamilyService cachedProductFamilyService;
    private final CachedApiDocService cachedApiDocService;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    /**
     * Create the controller and autowire in the repository services
     *
     * @param cachedProductFamilyService cached service for containers
     * @param cachedApiDocService        Cached state opf containers and services
     */
    @Autowired
    public ApiCatalogController(CachedProductFamilyService cachedProductFamilyService,
                                CachedApiDocService cachedApiDocService) {
        this.cachedProductFamilyService = cachedProductFamilyService;
        this.cachedApiDocService = cachedApiDocService;
    }


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
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "URI not found"),
        @ApiResponse(responseCode = "500", description = "An unexpected condition occurred")
    })
    public ResponseEntity<List<APIContainer>> getAllAPIContainers() throws ContainerStatusRetrievalThrowable {
        try {
            Iterable<APIContainer> allContainers = cachedProductFamilyService.getAllContainers();
            List<APIContainer> apiContainers = toList(allContainers);
            if (apiContainers == null || apiContainers.isEmpty()) {
                return new ResponseEntity<>(apiContainers, HttpStatus.NO_CONTENT);
            } else {
                // for each container, check the status of all it's services so it's overall status can be set here
                apiContainers.forEach(cachedProductFamilyService::calculateContainerServiceValues);
                return new ResponseEntity<>(apiContainers, HttpStatus.OK);
            }
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
            APIContainer containerById = cachedProductFamilyService.getContainerById(id);
            if (containerById != null) {
                apiContainers.add(containerById);
            }
            if (!apiContainers.isEmpty()) {
                apiContainers.forEach(apiContainer -> {
                    // For this single container, check the status of all it's services so it's overall status can be set here
                    cachedProductFamilyService.calculateContainerServiceValues(apiContainer);
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
                log.debug("An error occurred when trying to fetch ApiDoc for service: " + serviceId +
                    ", processing can continue but this service will not be able to display any Api Documentation.\n" +
                    "Error Message: " + e.getMessage());
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
