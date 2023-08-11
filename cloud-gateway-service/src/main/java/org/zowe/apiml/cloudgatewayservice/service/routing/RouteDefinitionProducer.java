/*
 * Copyright (c) 2022 Broadcom.  All Rights Reserved.  The term
 * "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This software and all information contained therein is
 * confidential and proprietary and shall not be duplicated,
 * used, disclosed, or disseminated in any way except as
 * authorized by the applicable license agreement, without the
 * express written permission of Broadcom.  All authorized
 * reproductions must be marked with this language.
 *
 * EXCEPT AS SET FORTH IN THE APPLICABLE LICENSE AGREEMENT, TO
 * THE EXTENT PERMITTED BY APPLICABLE LAW, BROADCOM PROVIDES THIS
 * SOFTWARE WITHOUT WARRANTY OF ANY KIND, INCLUDING WITHOUT
 * LIMITATION, ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT WILL BROADCOM
 * BE LIABLE TO THE END USER OR ANY THIRD PARTY FOR ANY LOSS OR
 * DAMAGE, DIRECT OR INDIRECT, FROM THE USE OF THIS SOFTWARE,
 * INCLUDING WITHOUT LIMITATION, LOST PROFITS, BUSINESS
 * INTERRUPTION, GOODWILL, OR LOST DATA, EVEN IF BROADCOM IS
 * EXPRESSLY ADVISED OF SUCH LOSS OR DAMAGE.
 */
package org.zowe.apiml.cloudgatewayservice.service.routing;

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

public abstract class RouteDefinitionProducer {

    protected final SimpleEvaluationContext evalCtxt = SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods().build();
    protected final Expression urlExpr;

    public RouteDefinitionProducer(DiscoveryLocatorProperties properties) {
        SpelExpressionParser parser = new SpelExpressionParser();
        urlExpr = parser.parseExpression(properties.getUrlExpression());
    }

    protected String evalHostname(ServiceInstance serviceInstance) {
        return urlExpr.getValue(this.evalCtxt, serviceInstance, String.class);
    }

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

    public abstract int getOrder();

    protected abstract void setCondition(RouteDefinition routeDefinition, ServiceInstance serviceInstance, RoutedService routedService);

    protected abstract void setFilters(RouteDefinition routeDefinition, ServiceInstance serviceInstance, RoutedService routedService);

    public RouteDefinition get(ServiceInstance serviceInstance, RoutedService routedService) {
        serviceInstance = getEvalServiceInstance(serviceInstance);
        RouteDefinition routeDefinition = buildRouteDefinition(serviceInstance, routedService.getSubServiceId());

        setCondition(routeDefinition, serviceInstance, routedService);
        setFilters(routeDefinition, serviceInstance, routedService);

        return routeDefinition;
    }

    @RequiredArgsConstructor
    static class ServiceInstanceEval implements ServiceInstance {

        @Delegate(excludes = Overridden.class)
        private final ServiceInstance original;
        private final String evalServiceId;

        @Override
        public String getServiceId() {
            return evalServiceId;
        }

        private static interface Overridden {
            String getServiceId();
        }

    }

}
