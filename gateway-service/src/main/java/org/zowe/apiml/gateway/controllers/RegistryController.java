/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.gateway.service.CentralApimlInfoMapper;
import org.zowe.apiml.gateway.service.GatewayIndexService;
import org.zowe.apiml.gateway.service.model.ApimlInfo;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.services.ServiceInfo;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.emptyToNull;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "Central Registry")
@RequestMapping(value = "/gateway/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(value = "apiml.gateway.registry.enabled", havingValue = "true")
@PreAuthorize("hasAuthority('TRUSTED_CERTIFICATE')")
public class RegistryController {

    private final CentralApimlInfoMapper centralApimlInfoMapper;
    private final GatewayIndexService gatewayIndexService;

    @GetMapping(value = {"/registry", "/registry/{apimlId}"})
    @Operation(summary = "Returns a list of services onboarded to the each instance of the APIML Discovery service.",
        operationId = "getServices",
        description = "Use the `/registry` API to list all APIML instances (central and domain) within their onboarded services. " +
            "Parameters `apiId` and `serviceId` are used to filter results.",
        security = {
            @SecurityRequirement(name = "ClientCert")
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful obtaining of services", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApimlInfo.class)
        )),
        @ApiResponse(responseCode = "403", description = "Client certificate is required", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiMessageView.class)
        ))
    })
    public Flux<ApimlInfo> getServices(@PathVariable(required = false) String apimlId, @Parameter(description = "Identifier of the API in the APIML") @RequestParam(name = "apiId", required = false) String apiId, @Parameter(description = "Service identifier") @RequestParam(name = "serviceId", required = false) String serviceId) {
        Map<String, List<ServiceInfo>> apimlList = gatewayIndexService.listRegistry(emptyToNull(apimlId), emptyToNull(apiId), emptyToNull(serviceId));
        return Flux.fromIterable(apimlList.entrySet())
            .map(this::buildEntry)
            .onErrorContinue(RuntimeException.class, (ex, consumer) -> log.debug("Unexpected mapping error", ex));
    }

    private ApimlInfo buildEntry(Map.Entry<String, List<ServiceInfo>> entry) {
        return centralApimlInfoMapper.buildApimlServiceInfo(entry.getKey(), entry.getValue());
    }

}
