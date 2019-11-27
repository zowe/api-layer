/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.swagger;

import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.apicatalog.swagger.api.AbstractApiDocService;
import com.ca.mfaas.apicatalog.swagger.api.ApiDocV2Service;
import com.ca.mfaas.apicatalog.swagger.api.ApiDocV3Service;
import com.ca.mfaas.product.gateway.GatewayClient;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.validation.UnexpectedTypeException;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TransformApiDocServiceTest {

    private final String SERVICE_ID = "SERVICE_1";

    private Function<String, AbstractApiDocService> beanApiDocFactory;
    private TransformApiDocService transformApiDocService;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setup() {
        ApiDocV2Service apiDocV2Service = mock(ApiDocV2Service.class);
        ApiDocV3Service apiDocV3Service = mock(ApiDocV3Service.class);

        beanApiDocFactory = content -> {
            if (content.equals("DOC2")) {
                return apiDocV2Service;
            } else if (content.equals("DOC3")) {
                return apiDocV3Service;
            } else {
                return null;
            }
        };

        transformApiDocService = new TransformApiDocService(beanApiDocFactory);
    }

    @Test
    public void testTransformApiDoc_whenThereIsNotApiDocMatch() {
        exceptionRule.expect(UnexpectedTypeException.class);
        exceptionRule.expectMessage("Response is not a Swagger or OpenAPI type object");

        ApiDocInfo apiDocInfo = new ApiDocInfo(null, "DOC4", null);
        transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);
    }

    @Test
    public void testTransformApiDoc_whenSwaggerDocIsPresent() {
        ApiDocInfo apiDocInfo = new ApiDocInfo(null, "DOC2", null);
        transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);


        verify(
            beanApiDocFactory.apply("DOC2")
        ).transformApiDoc(SERVICE_ID, apiDocInfo);

        verify(
            beanApiDocFactory.apply("DOC3"), never()
        ).transformApiDoc(SERVICE_ID, apiDocInfo);
    }

    @Test
    public void testTransformApiDoc_whenOpenDocIsPresent() {
        ApiDocInfo apiDocInfo = new ApiDocInfo(null, "DOC3", null);
        transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);


        verify(
            beanApiDocFactory.apply("DOC3")
        ).transformApiDoc(SERVICE_ID, apiDocInfo);

        verify(
            beanApiDocFactory.apply("DOC2"), never()
        ).transformApiDoc(SERVICE_ID, apiDocInfo);
    }
}
