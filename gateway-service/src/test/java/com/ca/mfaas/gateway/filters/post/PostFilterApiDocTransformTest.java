/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.filters.post;

import com.ca.mfaas.gateway.services.routing.RoutedService;
import com.ca.mfaas.gateway.services.routing.RoutedServices;
import com.netflix.zuul.context.RequestContext;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.validation.UnexpectedTypeException;

import java.io.IOException;

import static com.ca.mfaas.product.constants.ApimConstants.API_DOC_NORMALISED;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PROXY_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_URI_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

public class PostFilterApiDocTransformTest {

    private final String apiDocEndpoint = "/api-doc";
    private TransformApiDocEndpointsFilter filter;
    private RequestContext ctx;

    private String BASE_PATH  = "/api/v1/discovered-service";

    @Before
    public void setUp() {
        this.filter = new TransformApiDocEndpointsFilter();
        ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.set(REQUEST_URI_KEY, apiDocEndpoint);
        ctx.set(PROXY_KEY, "/api/v1/discovered-service");
        String serviceId = "discovered-service";
        ctx.set(SERVICE_ID_KEY, serviceId);
        ctx.setResponseBody(ApiDocController.apiDocResult);
        ctx.setResponseDataStream(IOUtils.toInputStream(ApiDocController.apiDocResult));
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        ctx.setResponse(response);
        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(
            new RoutedService(serviceId, "api/v1", "/discovered-service/v1"));
        this.filter.addRoutedServices(serviceId, routedServices);
    }

    @Test
    public void whenSendRequestToApiDoc_thenApiDocReturned() {
        RestAssuredMockMvc.standaloneSetup(new ApiDocController());
        given().
            param("group", "v1").
            when().
            get(apiDocEndpoint).
            then().
            statusCode(200).
            body("swagger", equalTo("2.0"));
    }

    @Test
    public void whenPostFilterApplied_thenSwaggerVersion2UnModified() throws IOException {
        final RequestContext ctx = RequestContext.getCurrentContext();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        ctx.setResponse(response);
        assertTrue(this.filter.shouldFilter());
        this.filter.run();
        String body = ctx.getResponseBody();
        assertEquals(apiDocEndpoint, ctx.get(REQUEST_URI_KEY));
        Swagger swagger = Json.mapper().readValue(body, Swagger.class);
        swagger.getPaths().forEach((endPoint, path) -> assertTrue(endPoint + " does not start with " + BASE_PATH, endPoint.startsWith(BASE_PATH)));
    }

    @Test
    public void whenRequestIsNotAnApiDocRequest_thenDoNotFilter() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(REQUEST_URI_KEY, "/hello");
        ctx.set(PROXY_KEY, "/hello");
        String body = "How's it going?";
        ctx.setResponseBody(body);
        assertFalse(this.filter.shouldFilter());
    }

    @Test(expected = UnexpectedTypeException.class)
    public void whenRequestIsAnApiDocRequestButNotSwagger_thenDoNotFilter() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(REQUEST_URI_KEY, apiDocEndpoint);
        ctx.set(PROXY_KEY, apiDocEndpoint);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        ctx.setResponse(response);
        String body = "How's it going?";
        ctx.setResponseBody(body);
        ctx.setResponseDataStream(IOUtils.toInputStream(body));
        assertTrue(this.filter.shouldFilter());
        this.filter.run();
    }

    @Test
    public void whenRequestIsAnApiDocRequestButAnErrorResponse_thenDoNotFilter() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(REQUEST_URI_KEY, apiDocEndpoint);
        ctx.set(PROXY_KEY, apiDocEndpoint);
        String body = "I have a bad feeling about this....";
        ctx.setResponseBody(body);
        ctx.setResponseDataStream(IOUtils.toInputStream(body));
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);
        ctx.setResponse(response);
        assertFalse(this.filter.shouldFilter());
    }

    @Test
    public void whenRequestIsAnApiDocRequestButAlreadyNormalised_thenDoNotFilter() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(REQUEST_URI_KEY, "/hello");
        ctx.set(PROXY_KEY, "/hello");
        ctx.set(API_DOC_NORMALISED, "true");
        String body = "do not normalise me";
        ctx.setResponseBody(body);
        assertFalse(this.filter.shouldFilter());
    }
}
