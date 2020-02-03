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

import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.swagger.api.AbstractApiDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.UnexpectedTypeException;
import java.util.function.Function;

/**
 * Transforms API documentation to documentation relative to Gateway, not the service instance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransformApiDocService {
    private final Function<String, AbstractApiDocService> beanApiDocFactory;

    /**
     * Does transformation API documentation
     *
     * @param serviceId  the unique service id
     * @param apiDocInfo the API doc and additional information about transformation
     * @return the transformed API documentation relative to Gateway
     * @throws ApiDocTransformationException if could not convert Swagger/OpenAPI to JSON
     * @throws UnexpectedTypeException       if response is not a Swagger/OpenAPI type object
     */
    public String transformApiDoc(String serviceId, ApiDocInfo apiDocInfo) {
        //maybe null check of apidocinfo
        AbstractApiDocService abstractApiDocService = beanApiDocFactory.apply(apiDocInfo.getApiDocContent());
        if (abstractApiDocService == null) {
            throw new UnexpectedTypeException("Response is not a Swagger or OpenAPI type object.");
        }

        return abstractApiDocService.transformApiDoc(serviceId, apiDocInfo);
    }
}
