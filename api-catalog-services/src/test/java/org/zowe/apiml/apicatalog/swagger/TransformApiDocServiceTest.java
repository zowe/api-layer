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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.swagger.api.AbstractApiDocService;
import org.zowe.apiml.apicatalog.swagger.api.ApiDocV2Service;
import org.zowe.apiml.apicatalog.swagger.api.ApiDocV3Service;

import jakarta.validation.UnexpectedTypeException;
import java.util.function.Function;

import static org.mockito.Mockito.*;

class TransformApiDocServiceTest {

    private final String SERVICE_ID = "SERVICE_1";

    private Function<String, AbstractApiDocService<?, ?>> beanApiDocFactory;
    private TransformApiDocService transformApiDocService;

    @BeforeEach
    void setup() {
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
    void testTransformApiDoc_whenThereIsNotApiDocMatch() {
        ApiDocInfo apiDocInfo = new ApiDocInfo(null, "DOC4", null);
        Exception exception = Assertions.assertThrows(UnexpectedTypeException.class, () -> {
            transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);
        });
        Assertions.assertEquals("Response is not a Swagger or OpenAPI type object.", exception.getMessage());
    }

    @Test
    void testTransformApiDoc_whenSwaggerDocIsPresent() {
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
    void testTransformApiDoc_whenOpenDocIsPresent() {
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
