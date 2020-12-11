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

import org.junit.jupiter.api.Test;

import javax.validation.UnexpectedTypeException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiTransformationConfigTest {

    private ApiTransformationConfig apiTransformationConfig = new ApiTransformationConfig(null);
    private Function<String, AbstractApiDocService> beanApiDocFactory =  apiTransformationConfig.beanApiDocFactory();

    @Test
    void testApiDocFactory_whenSwagerDocIsPresent() {
        AbstractApiDocService abstractApiDocService = beanApiDocFactory.apply("{\"swagger\": \"2.0\"}");
        assertTrue(abstractApiDocService instanceof ApiDocV2Service, "AbstractApiDocService is not ApiDocV2Service");
    }

    @Test
    void testApiDocFactory_whenOpenApiDocIsPresent() {
        AbstractApiDocService abstractApiDocService = beanApiDocFactory.apply("{\"openapi\": \"3.0\"}");
        assertTrue(abstractApiDocService instanceof ApiDocV3Service, "AbstractApiDocService is not ApiDocV3Service");
    }

    @Test
    void testApiDocFactory_whenApDocIsNotOpenApiNorSwagger() {
        AbstractApiDocService abstractApiDocService = beanApiDocFactory.apply("{\"superapi\": \"3.0\"}");
        assertNull(abstractApiDocService, "abstractApiDocService is not null");
    }

    @Test
    void testApiDocFactory_whenApDocVersionIsNotAsExpectedFormat() {
        Exception exception = assertThrows(UnexpectedTypeException.class, () -> {
            AbstractApiDocService abstractApiDocService = beanApiDocFactory.apply("FAILED FORMAT");
            assertNull(abstractApiDocService);  // This code is never reached
        });
        assertEquals("Response is not a Swagger or OpenAPI type object.", exception.getMessage());
    }

}
