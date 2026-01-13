/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.health;

import com.netflix.discovery.shared.Application;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.product.constants.CoreService;

import java.util.Optional;

/**
 * Caching service health information (/cachingservice/application/health)
 */
@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(name = "modulithConfig")
public class CachingHealthIndicator extends AbstractHealthIndicator {

    private final ApiMediationClient apiMediationClient;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        var eurekaClient = apiMediationClient.getEurekaClient();
        boolean gatewayUp = Optional.ofNullable(eurekaClient.getApplication(CoreService.GATEWAY.getServiceId())).map(Application::getInstances).map(i -> !i.isEmpty()).orElse(false);
        Status healthStatus = gatewayUp ? Status.UP : Status.DOWN;

        builder
            .status(healthStatus)
            .withDetail(CoreService.GATEWAY.getServiceId(), healthStatus.getCode());
    }

}
