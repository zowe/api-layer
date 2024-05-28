/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.web.ZuulController;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.product.compatibility.ApimlErrorController;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZuulErrorControllerHandlerTest {
    private static final String BEAN_NAME = "name";
    private static final String ERROR_PATH = "/error";

    private ZuulErrorControllerHandler underTest;

    private RouteLocator mockRouteLocator;
    private ZuulController mockZuulController;
    private ApimlErrorController mockErrorController;
    private MockHttpServletRequest mockRequest;

    private ZuulHandlerMapping zuulHandlerMapping;

    @BeforeEach
    void setup() {
        mockRequest = new MockHttpServletRequest();
        mockZuulController = mock(ZuulController.class);
        mockRouteLocator = mock(RouteLocator.class);

        mockErrorController = mock(ApimlErrorController.class);
        when(mockErrorController.getErrorPath()).thenReturn(ERROR_PATH);

        List<ApimlErrorController> mockErrorControllersList = new ArrayList<>();
        mockErrorControllersList.add(mockErrorController);

        underTest = new ZuulErrorControllerHandler(mockRouteLocator, mockZuulController, mockErrorControllersList);
        zuulHandlerMapping = new ZuulHandlerMapping(mockRouteLocator, mockZuulController);
    }

    @Nested
    class WhenPostProcess_ThenReturnOriginalBean {
        @Test
        void givenNullErrorController() {
            underTest = new ZuulErrorControllerHandler(mockRouteLocator, mockZuulController, null);
            Object newBean = underTest.postProcessAfterInitialization(zuulHandlerMapping, BEAN_NAME);
            assertSame(zuulHandlerMapping, newBean);
        }

        @Test
        void givenNotZuulHandlerMapping() {
            Object notZuulHandlerMapping = new Object();
            Object newBean = underTest.postProcessAfterInitialization(notZuulHandlerMapping, BEAN_NAME);
            assertSame(notZuulHandlerMapping, newBean);
        }
    }

    @Nested
    class WhenInvokeLookupHandler {
        @Test
        void givenEnhancedBean_thenNoError() {
            ZuulHandlerMapping enhancedZuulHandlerMapping = (ZuulHandlerMapping) underTest.postProcessAfterInitialization(zuulHandlerMapping, BEAN_NAME);
            assertNotNull(enhancedZuulHandlerMapping);
            enhancedZuulHandlerMapping.setErrorController(mockErrorController);

            assertDoesNotThrow(() -> invokeLookupHandler(enhancedZuulHandlerMapping, ERROR_PATH));
        }

        @Test
        void givenNotEnhancedBean_thenError() {
            zuulHandlerMapping.setErrorController(mockErrorController);
            InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> invokeLookupHandler(zuulHandlerMapping, ERROR_PATH));
            assertThat(e.getTargetException(), instanceOf(NoSuchMethodError.class));
        }

        private Object invokeLookupHandler(ZuulHandlerMapping instance, String path) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            Method lookupHandlerMethod = instance.getClass().getDeclaredMethod("lookupHandler", String.class, HttpServletRequest.class);
            lookupHandlerMethod.setAccessible(true);
            return lookupHandlerMethod.invoke(instance, path, mockRequest);
        }
    }
}
