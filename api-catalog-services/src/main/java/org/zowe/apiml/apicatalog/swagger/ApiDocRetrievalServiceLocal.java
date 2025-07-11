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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.core.jackson.mixin.MediaTypeMixin;
import io.swagger.v3.core.jackson.mixin.SchemaMixin;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.SpringDocCustomizers;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.SpringDocProviders;
import org.springdoc.core.service.AbstractRequestService;
import org.springdoc.core.service.GenericResponseService;
import org.springdoc.core.service.OpenAPIService;
import org.springdoc.core.service.OperationService;
import org.springdoc.webflux.api.OpenApiResource;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.zowe.apiml.config.ApiInfo;

import java.util.Locale;

@Slf4j
@Service
public class ApiDocRetrievalServiceLocal extends OpenApiResource {

    private ObjectMapper mapper = objectMapper();

    public ApiDocRetrievalServiceLocal(
        ObjectFactory<OpenAPIService> openAPIBuilderObjectFactory, AbstractRequestService requestBuilder,
        GenericResponseService responseBuilder, OperationService operationParser,
        SpringDocConfigProperties springDocConfigProperties,
        SpringDocProviders springDocProviders, SpringDocCustomizers springDocCustomizers
    ) {
        super(openAPIBuilderObjectFactory, requestBuilder, responseBuilder, operationParser, springDocConfigProperties, springDocProviders, springDocCustomizers);
        this.mapper = objectMapper();
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new SimpleModule().addSerializer(SecurityScheme.class, new SecuritySchemeSerializer()))
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            .addMixIn(Schema.class, SchemaMixin.class)
            .addMixIn(MediaType.class, MediaTypeMixin.class);
    }

    public ApiDocInfo retrieveApiDoc(ApiInfo apiInfo) {
        var openApi = getOpenApi(getServerUrl(null, apiInfo.getSwaggerUrl()), Locale.getDefault());

        try {
            return new ApiDocInfo(apiInfo, mapper.writeValueAsString(openApi), null);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize OpenAPI doc", e);
            return null;
        }
    }

    @Override
    protected String getServerUrl(ServerHttpRequest serverHttpRequest, String apiDocsUrl) {
        return apiDocsUrl;
    }

}
