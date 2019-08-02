package com.ca.mfaas.discovery;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import com.ca.mfaas.discovery.metadata.MetadataTranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EurekaInstanceRegisteredListener {
    private final MetadataTranslationService metadataTranslationService;

    @EventListener
    public void listen(EurekaInstanceRegisteredEvent event) {
        metadataTranslationService.translateMetadata(event.getInstanceInfo().getMetadata());
    }
}
