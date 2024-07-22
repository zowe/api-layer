/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.zaas.security.login.Providers;

import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;

/**
 * ZAAS health information (/application/health)
 */
@Slf4j
@Component
public class ZaasHealthIndicator extends AbstractHealthIndicator {
    private final Providers loginProviders;

    public ZaasHealthIndicator(Providers providers) {
        this.loginProviders = providers;
    }


    @Override
    protected void doHealthCheck(Health.Builder builder) {

        boolean authUp = true;
        if (loginProviders.isZosfmUsed()) {
            try {
                authUp = loginProviders.isZosmfAvailableAndOnline();
            } catch (AuthenticationServiceException ex) {
                log.error("The authentication service is not registered. Verify that it's properly configured and available.");
                System.exit(-1);
            }
        }

        builder.status(toStatus(authUp))
            .withDetail(CoreService.AUTH.getServiceId(), toStatus(authUp).getCode());

    }

    private Status toStatus(boolean up) {
        return up ? UP : DOWN;
    }
}
