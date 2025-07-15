/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.SpringDocCustomizers;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.SpringDocProviders;
import org.springdoc.core.service.AbstractRequestService;
import org.springdoc.core.service.GenericResponseService;
import org.springdoc.core.service.OpenAPIService;
import org.springdoc.core.service.OperationService;
import org.springdoc.webflux.api.OpenApiWebfluxResource;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.config.ApiInfo;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Service
public class ApiDocRetrievalServiceLocal {

    private Map<String, OpenApiWebfluxResource> apiDocResource = new HashMap<>();

    public ApiDocRetrievalServiceLocal(
        List<GroupedOpenApi> groupedOpenApis,
        ObjectFactory<OpenAPIService> openAPIBuilderObjectFactory,
        AbstractRequestService requestBuilder,
        GenericResponseService responseBuilder,
        OperationService operationParser,
        SpringDocConfigProperties springDocConfigProperties,
        SpringDocProviders springDocProviders
    ) {
        groupedOpenApis.stream()
            .forEach(groupedOpenApi -> {
                String group = groupedOpenApi.getGroup();

                SpringDocConfigProperties.GroupConfig groupConfig = new SpringDocConfigProperties.GroupConfig(group, groupedOpenApi.getPathsToMatch(), groupedOpenApi.getPackagesToScan(), groupedOpenApi.getPackagesToExclude(), groupedOpenApi.getPathsToExclude(), groupedOpenApi.getProducesToMatch(), groupedOpenApi.getConsumesToMatch(), groupedOpenApi.getHeadersToMatch(), groupedOpenApi.getDisplayName());
                springDocConfigProperties.addGroupConfig(groupConfig);

                var openApiWebfluxResource = new OpenApiWebfluxResource(groupedOpenApi.getGroup(),
                    openAPIBuilderObjectFactory,
                    requestBuilder,
                    responseBuilder,
                    operationParser,
                    springDocConfigProperties,
                    springDocProviders, new SpringDocCustomizers(Optional.of(groupedOpenApi.getOpenApiCustomizers()), Optional.of(groupedOpenApi.getOperationCustomizers()),
                    Optional.of(groupedOpenApi.getRouterOperationCustomizers()), Optional.of(groupedOpenApi.getOpenApiMethodFilters()))
                ) {
                    @Override
                    protected String getServerUrl(ServerHttpRequest serverHttpRequest, String apiDocsUrl) {
                        return "/";
                    }
                };

                apiDocResource.put(group, openApiWebfluxResource);
            });
    }

    public Mono<ApiDocInfo> retrieveApiDoc(InstanceInfo instanceInfo, ApiInfo apiInfo) {
        String serviceId = StringUtils.lowerCase(instanceInfo.getAppName());

        try {
            return Optional.ofNullable(apiDocResource.get(serviceId))
                .orElseThrow(() -> new ApiDocNotFoundException("Cannot obtain API doc for service " + serviceId))
                .openapiJson(null, "/", Locale.getDefault())
                .map(String::new)
                .map(content -> ApiDocInfo.builder().apiInfo(apiInfo).apiDocContent(content).build());
        } catch (JsonProcessingException jpe) {
            throw new ApiDocNotFoundException("Cannot obtain API doc for + " + serviceId, jpe);
        }
    }

}
