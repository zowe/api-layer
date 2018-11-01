/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.controllers.api;

import com.ca.mfaas.apicatalog.exceptions.ContainerStatusRetrievalException;
import com.ca.mfaas.apicatalog.model.APIContainer;
import com.ca.mfaas.apicatalog.services.cached.CachedProductFamilyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Main API for handling requests from the API Catalog UI, routed through the gateway
 */
@Slf4j
@RestController
@RequestMapping("/")
@Api(tags = {"API Catalog"},
    description = "Current state information")
public class ApiCatalogController {

    private final CachedProductFamilyService cachedProductFamilyService;

    /**
     * Create the controller and autowire in the repository services
     *
     * @param cachedProductFamilyService  cached service for containers
     */
    @Autowired
    public ApiCatalogController(CachedProductFamilyService cachedProductFamilyService) {
        this.cachedProductFamilyService = cachedProductFamilyService;
    }


    /**
     * Get all containers
     *
     * @return a list of all containers
     */
    @GetMapping(value = "/containers", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Lists catalog dashboard tiles",
        notes = "Returns a list of tiles including status and tile description"
    )
    public ResponseEntity<List<APIContainer>> getAllAPIContainers() throws ContainerStatusRetrievalException {
        try {
            Iterable<APIContainer> allContainers = cachedProductFamilyService.getAllContainers();
            List<APIContainer> apiContainers = toList(allContainers);
            if (apiContainers == null || apiContainers.isEmpty()) {
                return new ResponseEntity<>(apiContainers, HttpStatus.NO_CONTENT);
            } else {
                // for each container, check the status of all it's services so it's overall status can be set here
                apiContainers.forEach(cachedProductFamilyService::calculateContainerServiceTotals);
                return new ResponseEntity<>(apiContainers, HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error("Could not retrieve containers status: " + e.getMessage(), e);
            throw new ContainerStatusRetrievalException(e);
        }
    }

    /**
     * Get all containers (and included services)
     *
     * @return a containers by id
     */
    @GetMapping(value = "/containers/{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Retrieves a specific dashboard tile information",
        notes = "Returns information for a specific tile {id} including status and tile description"
    )
    public ResponseEntity<List<APIContainer>> getAPIContainerById(@PathVariable(value = "id") String id) throws ContainerStatusRetrievalException {
        try {
            List<APIContainer> apiContainers = new ArrayList<>();
            APIContainer containerById = cachedProductFamilyService.getContainerById(id);
            if (containerById != null) {
                apiContainers.add(containerById);
            }
            if (apiContainers.isEmpty()) {
                return new ResponseEntity<>(apiContainers, HttpStatus.OK);
            } else {
                // for each container, check the status of all it's services so it's overall status can be set here
                apiContainers.forEach(cachedProductFamilyService::calculateContainerServiceTotals);
                return new ResponseEntity<>(apiContainers, HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error("Could not retrieve container: " + e.getMessage(), e);
            throw new ContainerStatusRetrievalException(e);
        }
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
            .collect(Collectors.toList());
    }

}
