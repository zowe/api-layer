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

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.apicatalog.staticapi.StaticAPIResponse;
import org.zowe.apiml.apicatalog.staticapi.StaticDefinitionGenerator;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * Controller to handle the request issued from the UI to generate
 * a static definition file from the Wizard interface
 */
@RequiredArgsConstructor
@PreAuthorize("@safMethodSecurityExpressionRoot.hasSafServiceResourceAccess('SERVICES', 'READ',#root)")
public class StaticDefinitionController {

    private final StaticDefinitionGenerator staticDefinitionGenerator;

    /**
     * Retrieve the yaml from the request and store it in the file
     *
     * @param payload the request payload
     * @return the response entity
     */
    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<ResponseEntity<String>> generateStaticDef(@RequestBody String payload, @RequestHeader(value = "Service-Id") String serviceId) throws IOException {
        StaticAPIResponse staticAPIResponse = staticDefinitionGenerator.generateFile(payload, serviceId);
        return Mono.just(ResponseEntity
            .status(staticAPIResponse.getStatusCode())
            .body(staticAPIResponse.getBody()));
    }

    /**
     * Overwrite the file already created
     *
     * @param payload the request payload
     * @return the response entity
     */
    @PostMapping(value = "/override", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<ResponseEntity<String>> overrideStaticDef(@RequestBody String payload, @RequestHeader(value = "Service-Id") String serviceId) throws IOException {
        StaticAPIResponse staticAPIResponse = staticDefinitionGenerator.overrideFile(payload, serviceId);
        return Mono.just(ResponseEntity
            .status(staticAPIResponse.getStatusCode())
            .body(staticAPIResponse.getBody()));
    }

    @DeleteMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<ResponseEntity<String>> deleteStaticDef(@RequestHeader(value = "Service-Id") String serviceId) throws IOException {
        StaticAPIResponse staticAPIResponse = staticDefinitionGenerator.deleteFile(serviceId);
        return Mono.just(ResponseEntity.status(staticAPIResponse.getStatusCode()).body(staticAPIResponse.getBody()));
    }

}

@RestController
@RequestMapping("/apicatalog/api/v1/static-api")
@ConditionalOnBean(name = "modulithConfig")
class StaticDefinitionControllerModulith extends StaticDefinitionController {

    public StaticDefinitionControllerModulith(StaticDefinitionGenerator staticDefinitionGenerator) {
        super(staticDefinitionGenerator);
    }

}

@RestController
@RequestMapping("/apicatalog/static-api")
@ConditionalOnMissingBean(name = "modulithConfig")
class StaticDefinitionControllerMicroservice extends StaticDefinitionController {

    public StaticDefinitionControllerMicroservice(StaticDefinitionGenerator staticDefinitionGenerator) {
        super(staticDefinitionGenerator);
    }

}
