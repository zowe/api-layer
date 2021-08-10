/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.sse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.zowe.apiml.product.routing.RoutedServices;
import org.springframework.cloud.client.ServiceInstance;
import org.zowe.apiml.product.routing.RoutedServicesUser;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;


@Component("ServerSentEventProxyHandler")
public class ServerSentEventProxyHandler extends AbstractController implements RoutedServicesUser {

    private final Map<String, RoutedServices> routedServicesMap = new ConcurrentHashMap<>();
    private static final String SEPARATOR = "/";
    private final DiscoveryClient discovery;

    @Autowired
    public ServerSentEventProxyHandler(DiscoveryClient discovery) {
        this.discovery = discovery;
    }

    private String[] getUriParts(HttpServletRequest request, HttpServletResponse response) {
        String uriPath = request.getRequestURI();
        Map<String, String[]> parameters = request.getParameterMap();
        String arr[] = null;
        if (uriPath != null) {
            List<String> uriParts = new ArrayList<String>(Arrays.asList(uriPath.split("/", 6)));
            Iterator<Map.Entry<String, String[]>> it = (parameters.entrySet()).iterator();
            while (it.hasNext()) {
                Map.Entry<String, String[]> entry = (Map.Entry<String, String[]>)it.next();
                uriParts.add(entry.getKey() + "=" + entry.getValue()[0].toString());
            }
            arr = uriParts.toArray(new String[uriParts.size()]);
        }
        return arr;
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] uriParts = getUriParts(request, response);
        if (uriParts != null && uriParts.length >= 5) {
            String majorVersion = uriParts[3];
            String serviceId = uriParts[4];
            String path = uriParts[5];
            String params[] = Arrays.copyOfRange(uriParts, 6, uriParts.length);
            ServiceInstance serviceInstance = findServiceInstance(serviceId);
            String targetUrl = getTargetUrl(serviceId, serviceInstance, path, params);
            response.getWriter().print(targetUrl);
        }
        return null;
    }


    @Override
    public void addRoutedServices(String serviceId, RoutedServices routedServices) {
        // TODO Auto-generated method stub
        routedServicesMap.put(serviceId, routedServices);
    }

    private ServiceInstance findServiceInstance(String serviceId) {
        List<ServiceInstance> serviceInstances = this.discovery.getInstances(serviceId);
        if (!serviceInstances.isEmpty()) {
            // TODO: This just a simple implementation that will be replaced by more sophisticated mechanism in future
            return serviceInstances.get(0);
        } else {
            return null;
        }
    }

    private String getTargetUrl(String serviceId, ServiceInstance serviceInstance, String path, String params[]) {
        return "https" + "://" + serviceInstance.getHost() + ":"
            + serviceInstance.getPort() +
            SEPARATOR + serviceId + SEPARATOR + path + "?" + String.join("&", params);
    }
}
