/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.login.Providers;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class GatewayStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${apiml.startupCheckInterval:15}")
    private int interval;

    private AtomicReference<Timer> timer = new AtomicReference<>();

    private final Providers providers;

    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (providers.isZosfmUsed()) {
            timer.set(new Timer());
            timer.get().scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                    if (event.getApplicationContext() != null && !event.getApplicationContext().isActive()) {
                        cancel();
                        return;
                    }
                    if (providers.isZosmfAvailableAndOnline()) {
                        cancel();
                        notifyStartup();
                    }
                }

            }, 0, Duration.ofSeconds(interval).toMillis());
        } else {
            notifyStartup();
        }
    }

    void onContextClosed() {
        if (timer.get() != null) {
            timer.get().cancel();
            timer.set(null);
        }
    }

    private void notifyStartup() {
        new ServiceStartupEventHandler().onServiceStartup("Gateway Service",
            ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
    }

}
