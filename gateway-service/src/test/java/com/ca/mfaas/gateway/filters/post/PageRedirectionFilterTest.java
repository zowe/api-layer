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

import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import com.netflix.util.Pair;
import com.netflix.zuul.context.RequestContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

import static org.apache.http.HttpHeaders.LOCATION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

public class PageRedirectionFilterTest {

    private static final String SERVICE_ID = "discovered-service";
    private static final String TARGET_SERVER_HOST = "hostA.test.com";
    private static final int TARGET_SERVER_PORT = 8888;
    private static final String OTHER_SERVICE_ID = "other-service";
    private static final String OTHER_SERVICE_SERVER_HOST = "hostB.test.com";
    private static final int OTHER_SERVICE_SERVER_PORT = 9999;
    private static final String NOT_IN_DS_SERVER_HOST = "hostC.test.com";
    private static final int NOT_IN_DS_SERVER_PORT = 7777;
    private static final String URL_PREFIX = "/prefix1";

    private PageRedirectionFilter filter = null;
    private DiscoveryClient discoveryClient = null;
    private MockHttpServletResponse response = null;

    @Before
    public void setUp() {
        discoveryClient = mock(DiscoveryClient.class);
        this.filter = new PageRedirectionFilter(this.discoveryClient);

        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.set(SERVICE_ID_KEY, SERVICE_ID);
        MockHttpServletRequest request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        ctx.setRequest(request);
        ctx.setResponse(response);
    }

    //host and port in location are same as target server's
    //location contains service url
    @Test
    public void sameServerAndContainsGatewayURL() throws Exception {
        RoutedService currentService = new RoutedService("ui", "ui", "/");
        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(currentService);
        this.filter.addRoutedServices(SERVICE_ID, routedServices);

        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Arrays.asList(
            new DefaultServiceInstance(SERVICE_ID, TARGET_SERVER_HOST, TARGET_SERVER_PORT, true)
        ));

        response.setStatus(302);
        String relativePath = "/some/path/login.html";
        String location = mockLocationSameServer(relativePath);
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.addZuulResponseHeader(LOCATION, location);
        this.filter.run();

        Optional<Pair<String, String>> locationHeader = ctx.getZuulResponseHeaders()
            .stream()
            .filter(stringPair -> LOCATION.equals(stringPair.first()))
            .findFirst();

        verifyLocationUpdatedSameServer(locationHeader.get().second(), location,
            "/" + currentService.getGatewayUrl() + "/" + SERVICE_ID + relativePath);
    }

    //host and port in location are same as target server's
    //location does NOT contain service url
    @Test
    public void sameServerAndLocationNotContainGatewayURL() {
        String serviceUrl = "/discoverableclient/api/v1";
        RoutedService currentService = new RoutedService("api-v1", "api/v1", serviceUrl);
        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(currentService);
        this.filter.addRoutedServices(SERVICE_ID, routedServices);

        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Arrays.asList(
            new DefaultServiceInstance(SERVICE_ID, TARGET_SERVER_HOST, TARGET_SERVER_PORT, true)
        ));

        response.setStatus(304);
        String relativePath = "/some/path/login.html";
        String location = mockLocationSameServer(relativePath);

        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.addZuulResponseHeader(LOCATION, location);
        this.filter.run();

        Optional<Pair<String, String>> locationHeader = ctx.getZuulResponseHeaders()
            .stream()
            .filter(stringPair -> LOCATION.equals(stringPair.first()))
            .findFirst();

        verifyLocationNotUpdated(locationHeader.get().second(), location);
    }

    //host and port in location are NOT the same as target server's
    //host and port are registered in DS
    //location contains service url
    @Test
    public void differentServerAndHostPortInDSAndLocationContainsGatewayURL() throws Exception {
        //route for current service
        RoutedService currentService = new RoutedService("ui", "ui", "/");
        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(currentService);
        this.filter.addRoutedServices(SERVICE_ID, routedServices);
        //route for other service
        String serviceUrl = "/discoverableclient/api/v1";
        RoutedService otherService = new RoutedService("api-v1", "api/v1", serviceUrl);
        RoutedServices otherRoutedServices = new RoutedServices();
        otherRoutedServices.addRoutedService(otherService);
        this.filter.addRoutedServices(OTHER_SERVICE_ID, otherRoutedServices);

        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Arrays.asList(
            new DefaultServiceInstance(SERVICE_ID, TARGET_SERVER_HOST, TARGET_SERVER_PORT, true)
        ));
        when(discoveryClient.getInstances(OTHER_SERVICE_ID)).thenReturn(Arrays.asList(
            new DefaultServiceInstance(OTHER_SERVICE_ID, OTHER_SERVICE_SERVER_HOST, OTHER_SERVICE_SERVER_PORT, true)
        ));
        when(discoveryClient.getServices()).thenReturn(Arrays.asList(SERVICE_ID, OTHER_SERVICE_ID));

        response.setStatus(307);
        String relativePath = "/some/path/login.html";
        String location = mockLocationDSServer(serviceUrl + relativePath);

        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.addZuulResponseHeader(LOCATION, location);
        this.filter.run();

        Optional<Pair<String, String>> locationHeader = ctx.getZuulResponseHeaders()
            .stream()
            .filter(stringPair -> LOCATION.equals(stringPair.first()))
            .findFirst();

        this.verifyLocationUpdatedSameServer(locationHeader.get().second(), location,
            "/" + otherService.getGatewayUrl() + "/" + OTHER_SERVICE_ID + relativePath);
    }

    //host and port in location are NOT the same as target server's
    //host and port are NOT registered in DS
    //location contains gateway url (prefix + serviceId)
    @Test
    public void differentServerAndHostPortNotInDSAndLocationContainsGatewayURL() {
        //route for current service
        RoutedService currentService = new RoutedService("ui", "ui", "/");
        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(currentService);
        this.filter.addRoutedServices(SERVICE_ID, routedServices);
        //route for other service
        String serviceUrl = "/discoverableclient/api/v1";
        RoutedService otherService = new RoutedService("api-v1", "api/v1", serviceUrl);
        RoutedServices otherRoutedServices = new RoutedServices();
        otherRoutedServices.addRoutedService(otherService);
        this.filter.addRoutedServices(OTHER_SERVICE_ID, otherRoutedServices);

        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Arrays.asList(
            new DefaultServiceInstance(SERVICE_ID, TARGET_SERVER_HOST, TARGET_SERVER_PORT, true)
        ));
        when(discoveryClient.getInstances(OTHER_SERVICE_ID)).thenReturn(Arrays.asList(
            new DefaultServiceInstance(OTHER_SERVICE_ID, OTHER_SERVICE_SERVER_HOST, OTHER_SERVICE_SERVER_PORT, true)
        ));
        when(discoveryClient.getServices()).thenReturn(Arrays.asList(SERVICE_ID, OTHER_SERVICE_ID));

        response.setStatus(307);
        String relativePath = "/some/path/login.html";
        String location = mockLocationOtherServer(serviceUrl + relativePath);

        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.addZuulResponseHeader(LOCATION, location);
        this.filter.run();

        Optional<Pair<String, String>> locationHeader = ctx.getZuulResponseHeaders()
            .stream()
            .filter(stringPair -> LOCATION.equals(stringPair.first()))
            .findFirst();

        this.verifyLocationNotUpdated(locationHeader.get().second(), location);
    }

    //host and port in location are NOT the same as target server's
    //host and port are registered in DS
    //location does NOT contain gateway url (prefix + serviceId)
    @Test
    public void differentServerAndHostPortInDSAndLocationNotContainGatewayURL() {
        String serviceUrl = "/discoverableclient/api/v1";
        RoutedService currentService = new RoutedService("api-v1", "api/v1", serviceUrl);
        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(currentService);
        this.filter.addRoutedServices(SERVICE_ID, routedServices);

        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Arrays.asList(
            new DefaultServiceInstance(SERVICE_ID, TARGET_SERVER_HOST, TARGET_SERVER_PORT, true)
        ));
        when(discoveryClient.getInstances(OTHER_SERVICE_ID)).thenReturn(Arrays.asList(
            new DefaultServiceInstance(OTHER_SERVICE_ID, OTHER_SERVICE_SERVER_HOST, OTHER_SERVICE_SERVER_PORT, true)
        ));
        when(discoveryClient.getServices()).thenReturn(Arrays.asList(SERVICE_ID, OTHER_SERVICE_ID));

        response.setStatus(308);
        String relativePath = "/some/path/login.html";
        String location = mockLocationDSServer(relativePath);

        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.addZuulResponseHeader(LOCATION, location);
        this.filter.run();

        Optional<Pair<String, String>> locationHeader = ctx.getZuulResponseHeaders()
            .stream()
            .filter(stringPair -> LOCATION.equals(stringPair.first()))
            .findFirst();

        verifyLocationNotUpdated(locationHeader.get().second(), location);
    }

    //when service url end with slash
    @Test
    public void serviceUrlEndWithSlash() throws Exception {
        String serviceUrl = "/discoverableclient";
        RoutedService currentService = new RoutedService("api-v1", "api/v1", serviceUrl + "/");
        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(currentService);
        this.filter.addRoutedServices(SERVICE_ID, routedServices);

        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Arrays.asList(
            new DefaultServiceInstance(SERVICE_ID, TARGET_SERVER_HOST, TARGET_SERVER_PORT, true)
        ));

        response.setStatus(302);
        String relativePath = "/some/path/login.html";
        String location = mockLocationSameServer(serviceUrl + relativePath);
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.addZuulResponseHeader(LOCATION, location);
        this.filter.run();

        Optional<Pair<String, String>> locationHeader = ctx.getZuulResponseHeaders()
            .stream()
            .filter(stringPair -> LOCATION.equals(stringPair.first()))
            .findFirst();

        verifyLocationUpdatedSameServer(locationHeader.get().second(), location,
            "/" + currentService.getGatewayUrl() + "/" + SERVICE_ID + relativePath);
    }

    //registered url should be cached
    @Test
    public void urlCached() throws Exception {
        //run filter the first time to put url to cache
        String serviceUrl = "/discoverableclient";
        RoutedService currentService = new RoutedService("api-v1", "api/v1", serviceUrl);
        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(currentService);
        this.filter.addRoutedServices(SERVICE_ID, routedServices);

        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Arrays.asList(
            new DefaultServiceInstance(SERVICE_ID, TARGET_SERVER_HOST, TARGET_SERVER_PORT, true)
        ));

        response.setStatus(302);
        String relativePath = "/some/path/login.html";
        String location = mockLocationSameServer(serviceUrl + relativePath);
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.addZuulResponseHeader(LOCATION, location);
        this.filter.run();

        //clear context and run filter the second time to test cache
        discoveryClient = mock(DiscoveryClient.class);
        ctx.clear();
        ctx.set(SERVICE_ID_KEY, SERVICE_ID);
        MockHttpServletRequest request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        ctx.setRequest(request);
        ctx.setResponse(response);

        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Arrays.asList(
            new DefaultServiceInstance(SERVICE_ID, OTHER_SERVICE_SERVER_HOST, OTHER_SERVICE_SERVER_PORT, true)
        ));

        ctx.addZuulResponseHeader(LOCATION, location);
        this.filter.run();

        Optional<Pair<String, String>> locationHeader = ctx.getZuulResponseHeaders()
            .stream()
            .filter(stringPair -> LOCATION.equals(stringPair.first()))
            .findFirst();

        verifyLocationUpdatedSameServer(locationHeader.get().second(), location,
            "/" + currentService.getGatewayUrl() + "/" + SERVICE_ID + relativePath);
    }

    private String mockLocationSameServer(String relativeUrl) {
        return String.join("", "https://", TARGET_SERVER_HOST, ":", String.valueOf(TARGET_SERVER_PORT), relativeUrl);
    }

    private String mockLocationDSServer(String relativeUrl) {
        return String.join("", "https://", OTHER_SERVICE_SERVER_HOST, ":", String.valueOf(OTHER_SERVICE_SERVER_PORT), relativeUrl);
    }

    private String mockLocationOtherServer(String relativeUrl) {
        return String.join("", "https://", NOT_IN_DS_SERVER_HOST, ":", String.valueOf(NOT_IN_DS_SERVER_PORT), relativeUrl);
    }

//    private void mockDifferentServerButInDS(String location, String prefix) {
//        locationHeader = String.format(location, OTHER_SERVICE_SERVER_HOST, OTHER_SERVICE_SERVER_PORT, prefix);
//        //mock other service
//        when(discoveryClient.getInstances(OTHER_SERVICE_ID)).thenReturn(Arrays.asList(
//            new DefaultServiceInstance(OTHER_SERVICE_ID, OTHER_SERVICE_SERVER_HOST, OTHER_SERVICE_SERVER_PORT, true)
//        ));
//        when(discoveryClient.getServices()).thenReturn(Arrays.asList(SERVICE_ID, OTHER_SERVICE_ID));
//    }
//
//    private void mockDifferentServerAndNotInDS(String location, String prefix) {
//        locationHeader = String.format(location, OTHER_SERVICE_SERVER_HOST, OTHER_SERVICE_SERVER_PORT, prefix);
//        //mock other service
//        when(discoveryClient.getInstances(OTHER_SERVICE_ID)).thenReturn(Arrays.asList(
//            new DefaultServiceInstance(OTHER_SERVICE_ID, NOT_IN_DS_SERVER_HOST, NOT_IN_DS_SERVER_PORT, true)
//        ));
//        when(discoveryClient.getServices()).thenReturn(Arrays.asList(SERVICE_ID, OTHER_SERVICE_ID));
//    }
//
//    private String mockUrlPrefixMatched(RoutedService service) {
//        return service.getServiceUrl();
//    }

    private void verifyLocationUpdatedSameServer(String actualLocation, String originalLocation, String relativeUrl) throws Exception {
        RequestContext ctx = RequestContext.getCurrentContext();
        URI uri = new URI(originalLocation);
        uri = new URI(uri.getScheme(), uri.getUserInfo(), ctx.getRequest().getLocalName(), ctx.getRequest().getLocalPort(),
            relativeUrl, uri.getQuery(), uri.getFragment());
        assertEquals("Location header is not updated as expected", uri.toString(), actualLocation);
    }

    private void verifyLocationNotUpdated(String actualLocation, String expectedLocation) {
        assertEquals("Location should not be updated", expectedLocation, actualLocation);
    }

//    private void verifyLocationNotUpdated(String actualLocation) {
//        assertEquals("Location header should not be modified", this.locationHeader, actualLocation);
//    }

//    private void verifyGatewayUrlNotAddedToTable() {
//        HashSet<String> table = this.filter.getRegisteredUrls();
//        assertTrue(table.isEmpty());
//    }
}
