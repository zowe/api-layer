package com.ca.mfaas.product.routing;

import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import org.junit.Test;

import static org.junit.Assert.*;

public class TransformServiceTest {

    @Test
    public void transformURL() {
        //String url = "httrps://localhost:8080/ui";
        String url = "httfbdffvx";
        GatewayConfigProperties gateway = GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("gwhost")
            .build();
        String prefix = "u";
        String serviceId = "service";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(serviceId, prefix, "/ui");
        RoutedService routedService2 = new RoutedService(serviceId, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService();
        String actualUrl = transformService.transformURL(url, ServiceType.UI, routedServices, serviceId, gateway);

        String expectedUrl = String.format("%s://%s/%s/%s",
            gateway.getScheme(),
            gateway.getHostname(),
            prefix,
            serviceId);
        assertEquals(expectedUrl, actualUrl);
    }
}
