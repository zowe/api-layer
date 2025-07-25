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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.apicatalog.exceptions.ContainerStatusRetrievalException;
import org.zowe.apiml.apicatalog.model.APIContainer;
import org.zowe.apiml.apicatalog.model.APIService;
import org.zowe.apiml.apicatalog.swagger.ApiDocService;
import org.zowe.apiml.apicatalog.swagger.ContainerService;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Main API for handling requests from the API Catalog UI, routed through the gateway
 */
@Slf4j
@Tag(name = "API Catalog")
@RequiredArgsConstructor
public class ServicesController {

    private final ContainerService containerService;
    private final ApiDocService apiDocService;

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
    @ResponseBody
    public Mono<ResponseEntity<List<APIContainer>>> getAllAPIContainers() throws ContainerStatusRetrievalException {
        try {
            Iterable<APIContainer> allContainers = containerService.getAllContainers();
            List<APIContainer> apiContainers = toList(allContainers);
            if (apiContainers.isEmpty()) {
                // TODO: replace with 404
                return Mono.just(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
            }
            return Mono.just(ResponseEntity.ok(apiContainers));
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.apicatalog.containerCouldNotBeRetrieved", e.getMessage());
            throw new ContainerStatusRetrievalException(e);
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
    @ResponseBody
    public Mono<ResponseEntity<List<APIContainer>>> getAPIContainerById(@PathVariable(value = "id") String id) throws ContainerStatusRetrievalException {
        APIContainer containerById;
        try {
            containerById = containerService.getContainerById(id);
            if (containerById == null) {
                return Mono.just(new ResponseEntity<>(Collections.emptyList(), HttpStatus.NOT_FOUND));
            }
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.apicatalog.containerCouldNotBeRetrieved", e.getMessage());
            throw new ContainerStatusRetrievalException(e);
        }

        return Flux.fromIterable(containerById.getServices())
            .map(s -> {
                try {
                    s.setApiVersions(apiDocService.retrieveApiVersions(s.getServiceId()));
                    s.setDefaultApiVersion(apiDocService.retrieveDefaultApiVersion(s.getServiceId()));
                } catch (Exception e) {
                    log.debug("An error occurred when trying to fetch ApiDoc for service: {}, processing can continue but this service will not be able to display any Api Documentation.\nError:", s.getServiceId(), e);
                    s.setApiDocErrorMessage("Failed to fetch API documentation: " + e.getMessage());
                }
                return s;
            })
            .log()
            .flatMap(s ->
                Mono.zip(Mono.just(s), apiDocService.retrieveDefaultApiDoc(s.getServiceId())
                    .onErrorResume(Exception.class, e -> {
                        log.debug("An error occurred when trying to fetch ApiDoc for service: {}, processing can continue but this service will not be able to display any Api Documentation.\nError:", s.getServiceId(), e);
                        s.setApiDocErrorMessage("Failed to fetch API documentation: " + e.getMessage());
                        return Mono.empty();
                    })
                )
            )
            .map(x -> {
                x.getT1().setApiDoc(x.getT2());
                return x;
            })
            .then(Mono.just(containerById))
            .map(Collections::singletonList)
            .map(c -> new ResponseEntity<>(c, HttpStatus.OK));
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
    @ResponseBody
    public Mono<ResponseEntity<APIService>> getAPIServicesById(@PathVariable(value = "id") String id) {

        var service = containerService.getService(id);
        if (service == null) {
            return Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }
        log.debug("Getting service api doc by id {}", id);
        return apiDocService.retrieveDefaultApiDoc(id)
            .onErrorResume(e -> {
                log.debug("Cannot download api doc", e);
                return Mono.empty();
            })
            .map(apiDoc -> {
                log.debug("API doc was retrieved");
                service.setApiDoc(apiDoc);
                List<String> apiVersions = apiDocService.retrieveApiVersions(id);
                service.setApiVersions(apiVersions);
                log.debug("Got API versions: {}", apiVersions != null ? apiVersions.size() : 0);
                String defaultApiVersion = apiDocService.retrieveDefaultApiVersion(id);
                log.debug("Default API version: {}", defaultApiVersion);
                service.setDefaultApiVersion(defaultApiVersion);
                return service;
            })
            .switchIfEmpty(Mono.just(service))
            .map(s -> new ResponseEntity<>(s, HttpStatus.OK))
            .onErrorMap(Exception.class, e -> {
                apimlLog.log("org.zowe.apiml.apicatalog.serviceCouldNotBeRetrieved", e.getMessage());
                return new ContainerStatusRetrievalException(e);
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

@RestController
@RequestMapping("/apicatalog/api/v1/")
@ConditionalOnBean(name = "modulithConfig")
class ServicesControllerModulith extends ServicesController {

    public ServicesControllerModulith(ContainerService containerService, ApiDocService apiDocService) {
        super(containerService, apiDocService);
    }

}

@RestController
@RequestMapping("/apicatalog/")
@ConditionalOnMissingBean(name = "modulithConfig")
class ServicesControllerMicroservice extends ServicesController {

    public ServicesControllerMicroservice(ContainerService containerService, ApiDocService apiDocService) {
        super(containerService, apiDocService);
    }

}
