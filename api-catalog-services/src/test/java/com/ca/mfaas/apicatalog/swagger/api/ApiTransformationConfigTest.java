/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.swagger.api;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.validation.UnexpectedTypeException;
import java.util.function.Function;

import static org.junit.Assert.*;

public class ApiTransformationConfigTest {

    private ApiTransformationConfig apiTransformationConfig = new ApiTransformationConfig(null);
    private Function<String, AbstractApiDocService> beanApiDocFactory =  apiTransformationConfig.beanApiDocFactory();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testApiDocFactory_whenSwagerDocIsPresent() {
        AbstractApiDocService abstractApiDocService = beanApiDocFactory.apply("{\"swagger\": \"2.0\"}");
        assertTrue("AbstractApiDocService is not ApiDocV2Service", abstractApiDocService instanceof ApiDocV2Service);
    }

    @Test
    public void testApiDocFactory_whenOpenApiDocIsPresent() {
        AbstractApiDocService abstractApiDocService = beanApiDocFactory.apply("{\"openapi\": \"3.0\"}");
        assertTrue("AbstractApiDocService is not ApiDocV3Service", abstractApiDocService instanceof ApiDocV3Service);
    }

    @Test
    public void testApiDocFactory_whenApDocIsNotOpenApiNorSwagger() {
        AbstractApiDocService abstractApiDocService = beanApiDocFactory.apply("{\"superapi\": \"3.0\"}");
        assertNull("abstractApiDocService is not null", abstractApiDocService);
    }

    @Test
    public void testApiDocFactory_whenApDocVersionIsNotAsExpectedFormat() {
        exceptionRule.expect(UnexpectedTypeException.class);
        exceptionRule.expectMessage("Response is not a Swagger or OpenAPI type object");

        AbstractApiDocService abstractApiDocService = beanApiDocFactory.apply("FAILED FORMAT");
        assertNull(abstractApiDocService);
    }

}
