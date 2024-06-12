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
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.services.ServiceInfo;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.zowe.apiml.gateway.services.ServicesInfoService.CURRENT_VERSION;
import static org.zowe.apiml.gateway.services.ServicesInfoService.VERSION_HEADER;


@RestController
@RequiredArgsConstructor
@RequestMapping({ServicesInfoController.SERVICES_SHORT_URL, ServicesInfoController.SERVICES_FULL_URL})
@PreAuthorize("hasAuthority('TRUSTED_CERTIFICATE') or @safMethodSecurityExpressionRoot.hasSafServiceResourceAccess('SERVICES', 'READ',#root)")
public class ServicesInfoController {

    public static final String SERVICES_SHORT_URL = "/gateway/services";
    public static final String SERVICES_FULL_URL = "/gateway/api/v1/services";

    private final ServicesInfoService servicesInfoService;

    @GetMapping
    @ResponseBody
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
    public Mono<ResponseEntity<ServiceInfo>> getService(@PathVariable String serviceId) {
        ServiceInfo serviceInfo = servicesInfoService.getServiceInfo(serviceId);
        if (serviceInfo.getStatus() == InstanceInfo.InstanceStatus.UNKNOWN) {
            return Mono.just(ResponseEntity
                .status(NOT_FOUND)
                .header(VERSION_HEADER, CURRENT_VERSION)
                .build());
        }

        return Mono.just(ResponseEntity
            .ok()
            .header(VERSION_HEADER, CURRENT_VERSION)
            .contentType(MediaType.APPLICATION_JSON)
            .body(serviceInfo));
    }

}
