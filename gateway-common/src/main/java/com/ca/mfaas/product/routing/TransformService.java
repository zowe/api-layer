package com.ca.mfaas.product.routing;

import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class TransformService {

    public String transformURL(String serviceUrl,
                               ServiceType type,
                               RoutedServices routes,
                               String serviceId,
                               GatewayConfigProperties gateway) {
        URI serviceUri = URI.create(serviceUrl);

        RoutedService route = routes.getBestMatchingServiceUrl(serviceUri.getPath(), type);
        if (route == null) {
            log.warn("Not able to select route for url {} of the service {}. Original url used.",
                serviceUrl, serviceId);
            return serviceUrl;
        }
        String path = serviceUri.getPath().replace(route.getServiceUrl(), "");

        return String.format("%s://%s/%s/%s%s",
            gateway.getScheme(),
            gateway.getHostname(),
            route.getGatewayUrl(),
            serviceId,
            path);
    }
}
