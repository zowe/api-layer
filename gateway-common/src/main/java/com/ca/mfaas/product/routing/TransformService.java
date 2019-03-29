package com.ca.mfaas.product.routing;

import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;

@Slf4j
@Component
public class TransformService {

    private final GatewayConfigProperties gatewayConfigProperties;

    public TransformService(GatewayConfigProperties gatewayConfigProperties) {
        this.gatewayConfigProperties = gatewayConfigProperties;
    }

    public String transformURL(ServiceType type,
                               String serviceId,
                               String serviceUrl,
                               RoutedServices routes) {
        URI serviceUri = URI.create(serviceUrl);

        RoutedService route = routes.getBestMatchingServiceUrl(serviceUri.getPath(), type);
        if (route == null) {
            log.warn("Not able to select route for url {} of the service {}. Original url used.",
                serviceUrl, serviceId);
            return serviceUrl;
        }

        String path = serviceUri.getPath().replace(route.getServiceUrl(), "");

        return String.format("%s://%s/%s/%s%s",
            gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname(),
            route.getGatewayUrl(),
            serviceId,
            path);
    }
}
