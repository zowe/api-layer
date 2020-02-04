/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.filters.post;

import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.RoutedServicesUser;
import org.zowe.apiml.product.routing.ServiceType;
import org.zowe.apiml.product.routing.transform.TransformService;
import org.zowe.apiml.product.routing.transform.URLTransformationException;
import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.apache.http.HttpHeaders.LOCATION;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * This is a post filter for request which returns status code 3XX.
 * The filter checks Location in response header, and transforms the url to gateway url if the url satisfies 2 conditions:
 * <ul>
 * <li>Hostname and port of the url are registered in Discovery Service</li>
 * <li>The url can be matched to gateway url</li>
 * </ul>
 */
public class PageRedirectionFilter extends ZuulFilter implements RoutedServicesUser {

    private final DiscoveryClient discovery;
    private final Map<String, RoutedServices> routedServicesMap = new HashMap<>();
    private final TransformService transformService;
    private static final int MAX_ENTRIES = 1000;

    private final Map<String, String> routeTable = Collections.synchronizedMap(
        new LinkedHashMap<String, String>(MAX_ENTRIES + 1, .75F, true) {
            @Override
            public boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_ENTRIES;
            }
        }
    );

    /**
     * Constructor
     *
     * @param discovery               discovery client
     * @param gatewayConfigProperties gateway config properties
     */
    public PageRedirectionFilter(DiscoveryClient discovery, GatewayConfigProperties gatewayConfigProperties) {
        this.discovery = discovery;
        transformService = new TransformService(
            new GatewayClient(gatewayConfigProperties)
        );
    }

    /**
     * @return true if status code is 3XX
     */
    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();
        int status = context.getResponseStatusCode();
        return HttpStatus.valueOf(status).is3xxRedirection();
    }

    @Override
    public String filterType() {
        return POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return SEND_RESPONSE_FILTER_ORDER - 1;
    }

    /**
     * When the filter runs, it first finds the Location url in cache. If matched url can be found in cache, it then replaces Location with the matched url.
     * If not, the filter will find the matched url in Discovery Service. If matched url can be found in Discovery Service, the filter will put the matched
     * url to cache, and replace Location with the matched url
     *
     * @return null
     */
    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        Optional<Pair<String, String>> locationHeader = context.getZuulResponseHeaders()
            .stream()
            .filter(stringPair -> LOCATION.equals(stringPair.first()))
            .findFirst();

        if (locationHeader.isPresent()) {
            String location = locationHeader.get().second();
            //find matched url in cache
            String transformedUrl = foundUrlInTable(location);
            if (transformedUrl != null) {
                transformLocation(locationHeader.get(), transformedUrl);
            } else {
                //find matched url in Discovery Service
                Optional<String> transformedUrlOp = getMatchedUrlFromDS(location);
                if (transformedUrlOp.isPresent()) {
                    transformedUrl = transformedUrlOp.get();
                    //Put matched url to cache
                    routeTable.put(location, transformedUrl);
                    transformLocation(locationHeader.get(), transformedUrl);
                }
            }
        }

        return null;
    }

    /**
     * Find matched url in cache by location
     *
     * @param location url in Location header
     * @return matched url
     */
    private String foundUrlInTable(String location) {
        return routeTable.get(location);
    }

    /**
     * Find matched url Discovery Service.
     * First check the current service. If matched url can not be found in current service, the method then iterates
     * through all services registered in Discovery Service to find the matched url
     *
     * @param location url in Location header
     * @return return matched url if it can be found
     * return empty if matched url can not be found
     */
    private Optional<String> getMatchedUrlFromDS(String location) {
        RequestContext context = RequestContext.getCurrentContext();
        String currentServiceId = (String) context.get(SERVICE_ID_KEY);

        //check current service instance
        Optional<String> transformedUrl = foundMatchedUrlInService(location, currentServiceId);
        if (transformedUrl.isPresent()) {
            return transformedUrl;
        }

        //iterate through all instances registered in discovery client, check if there is matched url
        List<String> serviceIds = discovery.getServices();
        for (String serviceId : serviceIds) {
            if (!currentServiceId.equals(serviceId)) {
                transformedUrl = foundMatchedUrlInService(location, serviceId);
                if (transformedUrl.isPresent()) {
                    return transformedUrl;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Find matched url in specified service. For each service instance, the method first checks if the hostname and port
     * of the service instance are the same as the hostname and port in Location url. If they are the same, then try to
     * find the matched url.
     *
     * @param location  url in Location header
     * @param serviceId specified serviceId
     * @return return matched url if it can be found
     * return empty if matched url can not be found
     */
    private Optional<String> foundMatchedUrlInService(String location, String serviceId) {
        List<ServiceInstance> serviceInstances = discovery.getInstances(serviceId);
        for (ServiceInstance instance : serviceInstances) {
            //Check if the host and port in location is registered in DS
            String host = instance.getHost() + ":" + instance.getPort();
            if (location.contains(host)) {
                try {
                    String transformedUrl = transformService.transformURL(ServiceType.ALL, serviceId, location, routedServicesMap.get(serviceId));
                    return Optional.of(transformedUrl);
                } catch (URLTransformationException e) {
                    //do nothing if no matched url is found
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Replace Location header with transformed url
     *
     * @param locationHeader Location header
     * @param transformedUrl transformed url
     */
    private void transformLocation(Pair<String, String> locationHeader, String transformedUrl) {
        locationHeader.setSecond(transformedUrl);
    }

    @Override
    public void addRoutedServices(String serviceId, RoutedServices routedServices) {
        routedServicesMap.put(serviceId, routedServices);
    }
}
