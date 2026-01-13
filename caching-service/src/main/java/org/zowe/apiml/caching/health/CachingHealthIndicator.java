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
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.product.constants.CoreService;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Caching service health information (/cachingservice/application/health)
 */
@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(name = "modulithConfig")
public class CachingHealthIndicator extends AbstractHealthIndicator implements ApplicationListener<ApplicationReadyEvent> {

    private final AtomicReference<Boolean> serviceUp = new AtomicReference<>(false);

    private final ApiMediationClient apiMediationClient;
    private final Optional<CachesHealthIndicator> cachesHealthIndicator;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean gatewayUp = Optional.ofNullable(apiMediationClient.getEurekaClient())
            .map(eurekaClient -> eurekaClient.getApplication(CoreService.GATEWAY.getServiceId()))
            .map(Application::getInstances)
            .map(i -> !i.isEmpty())
            .orElse(false);
        builder.withDetail(CoreService.GATEWAY.getServiceId(), gatewayUp ? Status.UP : Status.DOWN);

        cachesHealthIndicator.ifPresent(i -> i.doHealthCheck(builder));
        if (!serviceUp.get() || !gatewayUp) {
            builder.down();
        }
    }

    @Override
    public void onApplicationEvent(@Nonnull final ApplicationReadyEvent event) {
        serviceUp.set(true);
    }

}
