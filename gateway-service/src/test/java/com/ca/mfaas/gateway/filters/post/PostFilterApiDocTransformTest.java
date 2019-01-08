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

    private static final String SERVICE_ID = "discovered-service";
    private static final String BASE_PATH = "/api/v1/discovered-service";
    private static final String ENDPOINT = "pets";

    private final String apiDocEndpoint = "/api-doc";
    private TransformApiDocEndpointsFilter filter;

    @Before
    public void setUp() {
        this.filter = new TransformApiDocEndpointsFilter();
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.set(REQUEST_URI_KEY, apiDocEndpoint);
        ctx.set(PROXY_KEY, "/api/v1/discovered-service");
        ctx.set(SERVICE_ID_KEY, SERVICE_ID);
        ctx.setResponseBody(ApiDocController.apiDocResult);
        ctx.setResponseDataStream(IOUtils.toInputStream(ApiDocController.apiDocResult));
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        ctx.setResponse(response);
        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(
            new RoutedService("v1", "api/v1", "/discovered-service/v1"));
        this.filter.addRoutedServices(SERVICE_ID, routedServices);
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
    public void whenPostFilterApplied_thenSwaggerVersionModified_oneGWUrl() throws IOException {
        final RequestContext ctx = RequestContext.getCurrentContext();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        ctx.setResponse(response);
        assertTrue(this.filter.shouldFilter());
        this.filter.run();
        String body = ctx.getResponseBody();
        assertEquals(apiDocEndpoint, ctx.get(REQUEST_URI_KEY));
        Swagger swagger = Json.mapper().readValue(body, Swagger.class);
        assertEquals("Base path for only one Gateway Url is not correct.", BASE_PATH, swagger.getBasePath());
        swagger.getPaths().forEach((endPoint, path) -> {
            assertTrue(endPoint + " does not contains 'pets'.", endPoint.contains(ENDPOINT));
            assertFalse(endPoint + " contains base path '" + BASE_PATH + "'.", endPoint.contains(SERVICE_ID));
        });
    }

    @Test
    public void whenPostFilterApplied_thenSwaggerVersionModified_twoGWUrls() throws IOException {
        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(
            new RoutedService("v1", "api/v1", "/discovered-service/v1"));
        routedServices.addRoutedService(
            new RoutedService("v2", "api/v2", "/discovered-service/v2"));
        this.filter.addRoutedServices(SERVICE_ID, routedServices);
        final RequestContext ctx = RequestContext.getCurrentContext();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        ctx.setResponse(response);
        assertTrue(this.filter.shouldFilter());
        this.filter.run();
        String body = ctx.getResponseBody();
        assertEquals(apiDocEndpoint, ctx.get(REQUEST_URI_KEY));
        Swagger swagger = Json.mapper().readValue(body, Swagger.class);
        assertTrue("Base path for two Gateway Urls is not empty.", swagger.getBasePath().isEmpty());
        swagger.getPaths().forEach((endPoint, path) -> {
            assertTrue(endPoint + " does not contains 'pets'.", endPoint.contains(ENDPOINT));
            assertTrue(endPoint + " does not contains base path '" + BASE_PATH + "'.", endPoint.contains(SERVICE_ID));
        });
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
        ctx.set(REQUEST_URI_KEY, "/api-doc");
        ctx.set(PROXY_KEY, "/hello");
        ctx.addZuulResponseHeader(API_DOC_NORMALISED, "true");
        String body = "do not normalise me";
        ctx.setResponseBody(body);
        assertFalse(this.filter.shouldFilter());
    }

    @Test
    public void whenRequestIsAnApiDocRequestButAlreadyNormalised_thenDoNotFilterAndRemoveZuulHeader() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(REQUEST_URI_KEY, "/api-doc");
        ctx.set(PROXY_KEY, "/hello");
        ctx.addZuulResponseHeader(API_DOC_NORMALISED, "true");
        ctx.addZuulResponseHeader("Leave-Me-Alone", "true");
        String body = "do not normalise me";
        ctx.setResponseBody(body);
        assertFalse(this.filter.shouldFilter());
        assertFalse(ctx.getZuulResponseHeaders().contains(API_DOC_NORMALISED));

    }
}
