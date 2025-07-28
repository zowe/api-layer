/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger.api;

import com.fasterxml.jackson.core.JsonParseException;
import jakarta.validation.UnexpectedTypeException;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.apicatalog.config.ApiTransformationConfig;
import org.zowe.apiml.config.ApplicationInfo;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ApiTransformationConfigTest {

    private final ApiTransformationConfig apiTransformationConfig = new ApiTransformationConfig(ApplicationInfo.builder().build(), null);
    private final Function<String, AbstractApiDocService<?, ?>> beanApiDocFactory = apiTransformationConfig.beanApiDocFactory();

    @Test
    void givenSwaggerJson_whenGetApiDocService_thenReturnApiDocV2Service() {
        var abstractApiDocService = beanApiDocFactory.apply("{\"swagger\": \"2.0\"}");
        assertTrue(abstractApiDocService instanceof ApiDocV2Service, "AbstractApiDocService is not ApiDocV2Service");
    }

    @Test
    void givenOpenApiJson_whenGetApiDocService_thenReturnApiDocV3Service() {
        var abstractApiDocService = beanApiDocFactory.apply("{\"openapi\": \"3.0\"}");
        assertTrue(abstractApiDocService instanceof ApiDocV3Service, "AbstractApiDocService is not ApiDocV3Service");
    }

    @Test
    void givenSwaggerYml_whenGetApiDocService_thenReturnApiDocV2Service() {
        var abstractApiDocService = beanApiDocFactory.apply("swagger: 2.0");
        assertTrue(abstractApiDocService instanceof ApiDocV2Service, "AbstractApiDocService is not ApiDocV2Service");
    }

    @Test
    void givenOpenApiYml_whenGetApiDocService_thenReturnApiDocV3Service() {
        var abstractApiDocService = beanApiDocFactory.apply("openapi: 3.0");
        assertTrue(abstractApiDocService instanceof ApiDocV3Service, "AbstractApiDocService is not ApiDocV3Service");
    }

    @Test
    void givenApiDocNotInOpenApiNorSwagger_whenGetApiDocService_thenReturnNull() {
        var abstractApiDocService = beanApiDocFactory.apply("{\"superapi\": \"3.0\"}");
        assertNull(abstractApiDocService, "abstractApiDocService is not null");
    }

    @Test
    void givenApDocVersionIsNotAsExpectedFormat_whenGetApiDocService_thenThrowException() {
        Exception exception = assertThrows(UnexpectedTypeException.class, () -> beanApiDocFactory.apply("FAILED FORMAT"));
        assertEquals("Response is not a Swagger or OpenAPI type object.", exception.getMessage());
    }

    @Test
    void givenJsonWithTabs_whenGetApiDocService_thenIsParsed() {
        var abstractApiDocService = beanApiDocFactory.apply("{\t\"openapi\": \"3.0\"}");
        assertTrue(abstractApiDocService instanceof ApiDocV3Service, "Parser doesn't support tabulators even it is JSON");
    }

    @Test
    void givenYamlWithTabs_whenGetApiDocService_thenItIsUnparseable() {
        var e = assertThrows(UnexpectedTypeException.class, () -> beanApiDocFactory.apply("\tswagger: 2.0"));
        assertInstanceOf(JsonParseException.class, e.getCause());
        assertTrue(e.getCause().getMessage().contains("Do not use \\t(TAB) for indentation"));
    }

}
