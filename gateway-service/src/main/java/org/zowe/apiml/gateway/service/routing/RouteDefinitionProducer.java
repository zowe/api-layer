/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.service.routing;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.util.StringUtils;
import org.zowe.apiml.product.routing.RoutedService;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_EXTERNAL_URL;

/**
 * The class that extends this abstract class is responsible to generate routing rule for a specific service and routing.
 *
 * The producers define an order ({@link #getOrder()}). It allows to create multiple rules with a prioritization.
 */
public abstract class RouteDefinitionProducer {

    protected final SimpleEvaluationContext evalCtxt = SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods().build();
    protected final Expression urlExpr;

    protected RouteDefinitionProducer(DiscoveryLocatorProperties properties) {
        SpelExpressionParser parser = new SpelExpressionParser();
        urlExpr = parser.parseExpression(properties.getUrlExpression());
    }

    protected String evalHostname(ServiceInstance serviceInstance) {
        return urlExpr.getValue(this.evalCtxt, serviceInstance, String.class);
    }

    /**
     * The method returns URL to route. In the case the service define URL by attribute apiml.service.externalUrl it is
     * reused. Otherwise, it generates default URL such as lb://<serviceId>
     * @param serviceInstance instance of service to process
     * @return URL for loadbalancer (without path)
     */
    protected String getHostname(ServiceInstance serviceInstance) {
        String output = null;
        Map<String, String> metadata = serviceInstance.getMetadata();
        if (metadata != null) {
            output = metadata.get(SERVICE_EXTERNAL_URL);
        }
        if (output == null) {
            output = evalHostname(serviceInstance);
        }
        return output;
    }

    protected ServiceInstance getEvalServiceInstance(ServiceInstance serviceInstance) {
        String serviceId = serviceInstance.getServiceId();

        Map<String, String> metadata = serviceInstance.getMetadata();
        if (metadata != null) {
            String apimlId = metadata.get(APIML_ID);
            if (StringUtils.hasText(apimlId)) {
                serviceId = apimlId;
            }
        }

        return new ServiceInstanceEval(serviceInstance, serviceId.toLowerCase());
    }

    protected RouteDefinition buildRouteDefinition(ServiceInstance serviceInstance, String routeId) {
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(serviceInstance.getInstanceId() + ":" + routeId);
        routeDefinition.setOrder(getOrder());
        routeDefinition.setUri(URI.create(getHostname(serviceInstance)));

        // add instance metadata
        routeDefinition.setMetadata(new LinkedHashMap<>(serviceInstance.getMetadata()));
        return routeDefinition;
    }

    /**
     * Define priority of the producer. A producer with lower number is processed before other one.
     * @return order defines the priority of producer
     */
    public abstract int getOrder();

    /**
     * Generates the condition which define if the rule could be used. Usually its checking if request is matching
     * to the routing rule (by path, header, etc.)
     * @param routeDefinition the rule created by this bean
     * @param serviceInstance instance of service
     * @param routedService routing definition
     */
    protected abstract void setCondition(RouteDefinition routeDefinition, ServiceInstance serviceInstance, RoutedService routedService);

    /**
     * Generates the action during routing for the service. It could be for example modification of path, header, etc.
     * @param routeDefinition the rule created by this bean
     * @param serviceInstance instance of service
     * @param routedService routing definition
     */
    protected abstract void setFilters(RouteDefinition routeDefinition, ServiceInstance serviceInstance, RoutedService routedService);

    /**
     * It creates a rule for specific input.
     * @param serviceInstance instance of service
     * @param routedService routing definition
     * @return definition of routing rule
     */
    public RouteDefinition get(ServiceInstance serviceInstance, RoutedService routedService) {
        serviceInstance = getEvalServiceInstance(serviceInstance);
        RouteDefinition routeDefinition = buildRouteDefinition(serviceInstance, routedService.getSubServiceId());

        setCondition(routeDefinition, serviceInstance, routedService);
        setFilters(routeDefinition, serviceInstance, routedService);

        return routeDefinition;
    }

    /**
     * This class is responsible to update ServiceInstance without changing data in the origin. It can change
     * the serviceId. It is helpful if we want to use a different identifier instead of serviceId, ie. ID of APIML.
     */
    @RequiredArgsConstructor
    static class ServiceInstanceEval implements ServiceInstance {

        @Delegate(excludes = Overridden.class)
        private final ServiceInstance original;
        private final String evalServiceId;

        @Override
        public String getServiceId() {
            return evalServiceId;
        }

        private interface Overridden {
            String getServiceId();
        }

    }

}
