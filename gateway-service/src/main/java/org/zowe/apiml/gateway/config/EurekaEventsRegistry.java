/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.config;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class EurekaEventsRegistry {

    private final EurekaClient eurekaClient;
    private final List<EurekaEventListener> processors;

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        processors.forEach(eurekaClient::registerEventListener);
    }
}
