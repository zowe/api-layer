/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discovery;

import com.netflix.appinfo.InstanceInfo;
import org.zowe.apiml.discovery.metadata.MetadataDefaultsService;
import org.zowe.apiml.discovery.metadata.MetadataTranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.zowe.apiml.util.EurekaUtils;

import java.util.Map;

/**
 * Called by Eureka when the service instance is registered
 */
@Component
@RequiredArgsConstructor
public class EurekaInstanceRegisteredListener {

    private final MetadataTranslationService metadataTranslationService;
    private final MetadataDefaultsService metadataDefaultsService;
    private final GatewayNotifier gatewayNotifier;

    /**
     * Translates service instance Eureka metadata from older versions to the current version
     */
    @EventListener
    public void listen(EurekaInstanceRegisteredEvent event) {
        final InstanceInfo instanceInfo = event.getInstanceInfo();
        final Map<String, String> metadata = instanceInfo.getMetadata();
        final String serviceId = EurekaUtils.getServiceIdFromInstanceId(instanceInfo.getInstanceId());

        metadataTranslationService.translateMetadata(serviceId, metadata);
        metadataDefaultsService.updateMetadata(serviceId, metadata);
        // ie. new instance can have different authentication (than other one), this is reason to evict caches on gateway
        gatewayNotifier.serviceUpdated(serviceId);
    }

}
