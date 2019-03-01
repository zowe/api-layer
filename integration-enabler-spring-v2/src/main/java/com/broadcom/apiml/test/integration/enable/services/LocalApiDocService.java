/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.test.integration.enable.services;

import io.swagger.models.Swagger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import springfox.documentation.service.Documentation;
import springfox.documentation.spring.web.DocumentationCache;
import springfox.documentation.spring.web.json.Json;
import springfox.documentation.spring.web.json.JsonSerializer;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.mappers.ServiceModelToSwagger2Mapper;

import java.util.*;

@Service
@ConditionalOnProperty(prefix = "eureka.instance.metadata-map.mfaas.discovery", value = "enableApiDoc", havingValue = "true", matchIfMissing = true)
public class LocalApiDocService {

    private final DocumentationCache documentationCache;
    private final JsonSerializer jsonSerializer;
    private final ServiceModelToSwagger2Mapper mapper;

    /**
     * API Doc retrieval controller
     * Autowire in dependencies to contoller
     *
     * @param documentationCache in memory cache of discovered API Docs
     * @param jsonSerializer     json serializer
     * @param mapper             json mapper
     */
    @Autowired
    public LocalApiDocService(
        DocumentationCache documentationCache,
        JsonSerializer jsonSerializer,
        ServiceModelToSwagger2Mapper mapper) {
        this.jsonSerializer = jsonSerializer;
        this.mapper = mapper;
        this.documentationCache = documentationCache;
    }

    /**
     * Retrieve the API doc for this service
     *
     * @param apiDocGroup the API doc group to retrieve (can be null, the apply default)
     * @return the swagger as a string
     */
    public String getApiDoc(String apiDocGroup) {
        String groupName = Optional.ofNullable(apiDocGroup).orElse(Docket.DEFAULT_GROUP_NAME);
        Documentation documentation = documentationCache.documentationByGroup(groupName);
        if (documentation == null) {

            // default to lowest alphabetically sorted group name
            List<Map.Entry<String, Documentation>> entries = new ArrayList<>(documentationCache.all().entrySet());
            entries.sort(Comparator.comparing(o -> o.getValue().getGroupName()));
            Map.Entry<String, Documentation> firstEntry = entries.get(0);
            if (firstEntry == null) {
                return "Could not find group: " + groupName;
            } else {
                documentation = firstEntry.getValue();
            }
        }
        Swagger apiDoc = mapper.mapDocumentation(documentation);
        Json json = jsonSerializer.toJson(apiDoc);
        return json.value();
    }
}
