/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.product.routing;

import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.MalformedURLException;

import static org.junit.Assert.*;

public class TransformServiceTest {

    public static final String UI_PREFIX = "ui";
    public static final String API_PREFIX = "api";
    public static final String WS_PREFIX = "ws";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldTransformURL() throws URLTransformationException {
        String url = "https://localhost:8080/ui";
        GatewayConfigProperties gateway = GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost")
            .build();
        String serviceId = "service";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(serviceId, UI_PREFIX, "/ui");
        RoutedService routedService2 = new RoutedService(serviceId, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gateway);
        String actualUrl = transformService.transformURL(ServiceType.UI, serviceId, url, routedServices);

        String expectedUrl = String.format("%s://%s/%s/%s",
            gateway.getScheme(),
            gateway.getHostname(),
            UI_PREFIX,
            serviceId);
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    public void shouldUseOriginalUrl_IfRouteNotFound() throws URLTransformationException {
        String url = "https://localhost:8080/u";
        GatewayConfigProperties gateway = GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost")
            .build();
        String serviceId = "service";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(serviceId, UI_PREFIX, "/ui");
        RoutedService routedService2 = new RoutedService(serviceId, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gateway);
        String actualUrl = transformService.transformURL(ServiceType.UI, serviceId, url, routedServices);

        assertEquals(url, actualUrl);
    }

    @Test
    public void shouldSelectWsRoute() throws URLTransformationException {
        String url = "https://localhost:8080/ws";
        GatewayConfigProperties gateway = GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost")
            .build();
        String serviceId = "service";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(serviceId, WS_PREFIX, "/ws");
        RoutedService routedService2 = new RoutedService(serviceId, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gateway);
        String actualUrl = transformService.transformURL(ServiceType.WS, serviceId, url, routedServices);

        String expectedUrl = String.format("%s://%s/%s/%s",
            gateway.getScheme(),
            gateway.getHostname(),
            WS_PREFIX,
            serviceId);
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    public void shouldSelectApiRoute() throws URLTransformationException {
        String url = "https://localhost:8080/api";
        GatewayConfigProperties gateway = GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost")
            .build();
        String serviceId = "service";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(serviceId, API_PREFIX, "/api");
        RoutedService routedService2 = new RoutedService(serviceId, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gateway);
        String actualUrl = transformService.transformURL(ServiceType.API, serviceId, url, routedServices);

        String expectedUrl = String.format("%s://%s/%s/%s",
            gateway.getScheme(),
            gateway.getHostname(),
            API_PREFIX,
            serviceId);
        assertEquals(expectedUrl, actualUrl);
    }


    @Test
    public void shouldSelectOriginalUrl_IfPathdIsNotValid() throws URLTransformationException {
        String url = "https://localhost:8080/wss";
        GatewayConfigProperties gateway = GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost")
            .build();
        String serviceId = "service";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(serviceId, WS_PREFIX, "/ws");
        RoutedService routedService2 = new RoutedService(serviceId, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gateway);

        exception.expect(MalformedURLException.class);
        exception.expectMessage("The path /wss of the service URL https://localhost:8080/wss is not valid.");
        transformService.transformURL(ServiceType.WS, serviceId, url, routedServices);
    }

}
