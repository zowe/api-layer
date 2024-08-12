/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.services;

import com.netflix.appinfo.InstanceInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.gateway.service.model.ApimlInfo;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.services.ServiceInfo;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.zowe.apiml.gateway.services.ServicesInfoService.CURRENT_VERSION;
import static org.zowe.apiml.gateway.services.ServicesInfoService.VERSION_HEADER;


@RestController
@RequiredArgsConstructor
@Tag(name = "Services")
@RequestMapping({ServicesInfoController.SERVICES_SHORT_URL, ServicesInfoController.SERVICES_FULL_URL})
@PreAuthorize("hasAuthority('TRUSTED_CERTIFICATE') or @safMethodSecurityExpressionRoot.hasSafServiceResourceAccess('SERVICES', 'READ',#root)")
public class ServicesInfoController {

    public static final String SERVICES_SHORT_URL = "/gateway/services";
    public static final String SERVICES_FULL_URL = "/gateway/api/v1/services";

    private final ServicesInfoService servicesInfoService;

    @GetMapping
    @ResponseBody
    @Operation(summary = "Returns a list of services onboarded to this APIML instance", operationId = "getServices", security = {
        @SecurityRequirement(name = "ClientCert")
    })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful obtaining of services", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApimlInfo.class)
        )),
        @ApiResponse(responseCode = "404", description = "No service was found", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiMessageView.class)
        ))
    })
    public Mono<ResponseEntity<List<ServiceInfo>>> getServices(@RequestParam(required = false) String apiId) {
        List<ServiceInfo> services = servicesInfoService.getServicesInfo(apiId);

        if (services.isEmpty()) {
            return Mono.just(ResponseEntity
                .status(NOT_FOUND)
                .header(VERSION_HEADER, CURRENT_VERSION)
                .build());
        }

        return Mono.just(ResponseEntity
            .ok()
            .header(VERSION_HEADER, CURRENT_VERSION)
            .contentType(MediaType.APPLICATION_JSON)
            .body(services));
    }

    @GetMapping("/{serviceId}")
    @ResponseBody
    @Operation(summary = "Return information about a specified service", operationId = "getServices", security = {
        @SecurityRequirement(name = "ClientCert")
    })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful obtaining of services", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApimlInfo.class)
        )),
        @ApiResponse(responseCode = "404", description = "No service was found", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiMessageView.class)
        ))
    })
    public Mono<ResponseEntity<ServiceInfo>> getService(@PathVariable String serviceId) {
        ServiceInfo serviceInfo = servicesInfoService.getServiceInfo(serviceId);
        var status = (serviceInfo.getStatus() == InstanceInfo.InstanceStatus.UNKNOWN) ? NOT_FOUND : OK;

        return Mono.just(ResponseEntity
            .status(status)
            .header(VERSION_HEADER, CURRENT_VERSION)
            .contentType(MediaType.APPLICATION_JSON)
            .body(serviceInfo));
    }

}
