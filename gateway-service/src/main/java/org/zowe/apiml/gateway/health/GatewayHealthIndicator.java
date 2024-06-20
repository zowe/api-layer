/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.zowe.apiml.gateway.security.login.Providers;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.constants.CoreService;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;

/**
 * Gateway health information (/application/health)
 */
@Component
@Slf4j
public class GatewayHealthIndicator extends AbstractHealthIndicator {
    private final DiscoveryClient discoveryClient;
    private final Providers loginProviders;
    private final String apiCatalogServiceId;

    private final ApimlLogger apimlLog = ApimlLogger.of(GatewayHealthIndicator.class,
            YamlMessageServiceInstance.getInstance());
    boolean startedInformationPublished = false;

    public GatewayHealthIndicator(DiscoveryClient discoveryClient,
                                  Providers providers,
                                  @Value("${apiml.catalog.serviceId:}") String apiCatalogServiceId) {
        this.discoveryClient = discoveryClient;
        this.loginProviders = providers;
        this.apiCatalogServiceId = apiCatalogServiceId;
    }


    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean anyCatalogIsAvailable = apiCatalogServiceId != null && !apiCatalogServiceId.isEmpty();
        boolean apiCatalogUp = !this.discoveryClient.getInstances(apiCatalogServiceId).isEmpty();
        // When DS goes 'down' after it was already 'up', the new status is not shown. This is probably feature of
        // Eureka client which caches the status of services. When DS is down the cache is not refreshed.
        boolean discoveryUp = !this.discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId()).isEmpty();

        boolean authUp = true;
        if (loginProviders.isZosfmUsed()) {
            try {
                authUp = loginProviders.isZosmfAvailableAndOnline();
            } catch (AuthenticationServiceException ex) {
                System.exit(-1);
            }
        }

        int gatewayCount = this.discoveryClient.getInstances(CoreService.GATEWAY.getServiceId()).size();
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        boolean isAuthenticated = request.getUserPrincipal() != null; // Check if the user is authenticated
        log.error("AUTHENTICATED {}", isAuthenticated);
        if (isAuthenticated) {
            builder.status(toStatus(discoveryUp))
                .withDetail(CoreService.DISCOVERY.getServiceId(), toStatus(discoveryUp).getCode())
                .withDetail(CoreService.AUTH.getServiceId(), toStatus(authUp).getCode())
                .withDetail("gatewayCount", gatewayCount);
        } else {
            builder.status(toStatus(discoveryUp))
                .withDetail("gatewayCount", gatewayCount);
        }

        if (anyCatalogIsAvailable) {
            builder.withDetail(CoreService.API_CATALOG.getServiceId(), toStatus(apiCatalogUp).getCode());
        }

        if (!startedInformationPublished && discoveryUp && apiCatalogUp && authUp) {
            apimlLog.log("org.zowe.apiml.common.mediationLayerStarted");
            startedInformationPublished = true;
        }
    }

    private Status toStatus(boolean up) {
        return up ? UP : DOWN;
    }
}
