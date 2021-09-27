package org.zowe.apiml.gateway.routing;

import lombok.experimental.UtilityClass;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;
import java.util.Optional;

@UtilityClass
public class RouteUtil {

    /**
     * Find a ServiceInstance for the service ID in a Gateway URI.
     *
     * @param requestUri      Gateway URI.
     * @param discoveryClient DiscoveryClient instance with registered services.
     * @return Optional containing the first matching ServiceInstance. If the DiscoveryClient has no matching ServiceInstance,
     * returns an empty Optional.
     */
    public static Optional<ServiceInstance> getInstanceInfoForUri(String requestUri, DiscoveryClient discoveryClient) {
        String[] uriParts = requestUri.split("/");
        List<ServiceInstance> instances;
        if (uriParts.length < 2) {
            return Optional.empty();
        }
        if ("api".equals(uriParts[1]) || "ui".equals(uriParts[1])) {
            if (uriParts.length < 4) {
                return Optional.empty();
            }
            instances = discoveryClient.getInstances(uriParts[3]);
        } else {
            instances = discoveryClient.getInstances(uriParts[1]);
        }
        if (instances == null || instances.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(instances.get(0));
    }
}
