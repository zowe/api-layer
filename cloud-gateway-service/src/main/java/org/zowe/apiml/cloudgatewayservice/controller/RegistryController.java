/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.cloudgatewayservice.service.CentralApimlInfoMapper;
import org.zowe.apiml.cloudgatewayservice.service.GatewayIndexService;
import org.zowe.apiml.cloudgatewayservice.service.model.ApimlInfo;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.services.ServiceInfo;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.emptyToNull;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "Central Registry")
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(value = "apiml.cloudGateway.serviceRegistryEnabled", havingValue = "true")
public class RegistryController {

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();
    private final CentralApimlInfoMapper centralApimlInfoMapper;
    private final GatewayIndexService gatewayIndexService;

    @GetMapping(value = {"/registry/", "/registry", "/registry/{apimlId}"})
    public Flux<ApimlInfo> getServices(@PathVariable(required = false) String apimlId, @RequestParam(name = "apiId", required = false) String apiId, @RequestParam(name = "serviceId", required = false) String serviceId) {
        Map<String, List<ServiceInfo>> apimlList = gatewayIndexService.listRegistry(emptyToNull(apimlId), emptyToNull(apiId), emptyToNull(serviceId));
        return Flux.fromIterable(apimlList.entrySet()).map(this::buildEntry).onErrorContinue(RuntimeException.class, (ex, consumer) -> log.debug("Unexpected mapping error", ex));
    }

    private ApimlInfo buildEntry(Map.Entry<String, List<ServiceInfo>> entry) {
        return centralApimlInfoMapper.buildApimlServiceInfo(entry.getKey(), entry.getValue());
    }

}
