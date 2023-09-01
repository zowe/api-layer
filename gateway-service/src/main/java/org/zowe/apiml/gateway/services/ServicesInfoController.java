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
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.services.ServiceInfo;
import org.zowe.apiml.product.services.ServicesInfoService;

import java.util.List;

import static org.zowe.apiml.product.services.ServicesInfoService.CURRENT_VERSION;
import static org.zowe.apiml.product.services.ServicesInfoService.VERSION_HEADER;


@RestController
@RequiredArgsConstructor
@RequestMapping(ServicesInfoController.SERVICES_URL)
@PreAuthorize("hasAuthority('TRUSTED_CERTIFICATE') or hasSafServiceResourceAccess('SERVICES', 'READ')")
public class ServicesInfoController {

    public static final String SERVICES_URL = "/gateway/services";

    private final ServicesInfoService servicesInfoService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @HystrixCommand
    public ResponseEntity<List<ServiceInfo>> getServices(@RequestParam(required = false) String apiId) {
        List<ServiceInfo> services = servicesInfoService.getServicesInfo(apiId);
        HttpStatus status = (services.isEmpty()) ? HttpStatus.NOT_FOUND : HttpStatus.OK;

        return ResponseEntity
                .status(status)
                .header(VERSION_HEADER, CURRENT_VERSION)
                .body(services);
    }

    @GetMapping(value = "/{serviceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @HystrixCommand
    public ResponseEntity<ServiceInfo> getService(@PathVariable String serviceId) {
        ServiceInfo serviceInfo = servicesInfoService.getServiceInfo(serviceId);
        HttpStatus status = (serviceInfo.getStatus() == InstanceInfo.InstanceStatus.UNKNOWN) ?
                HttpStatus.NOT_FOUND : HttpStatus.OK;

        return ResponseEntity
                .status(status)
                .header(VERSION_HEADER, CURRENT_VERSION)
                .body(serviceInfo);
    }

}
