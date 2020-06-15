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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.zowe.apiml.gateway.metadata.service.MetadataProcessor;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class EurekaEventsRegistry {

    private final EurekaClient eurekaClient;
    private final MetadataProcessor processor;

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        eurekaClient.registerEventListener(processor);
    }
}
