/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;

@Component
@RequiredArgsConstructor
public class CachingServiceStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private final ServiceStartupEventHandler handler;

    @Override
    public void onApplicationEvent(@Nonnull final ApplicationReadyEvent event) {
        handler.onServiceStartup("Caching Service",
            ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
    }

}
