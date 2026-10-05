/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry.interceptor;

import lombok.RequiredArgsConstructor;
import org.zowe.apiml.discovery.metadata.MetadataDefaultsService;
import org.zowe.apiml.discovery.metadata.MetadataTranslationService;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.registry.spi.RegistrationInterceptor;
import org.zowe.apiml.util.EurekaUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Brings older metadata up to the current version and applies any configured service overrides.
 * <p>
 * This ran as a post-registration event listener under Eureka, mutating the metadata map of an instance that was
 * already in the registry. That worked only because Netflix's {@code InstanceInfo} exposes a mutable map, and it
 * left a window - short, but real - in which the Gateway could read an instance whose V1 metadata had not yet
 * been translated and so build a route with no gateway URL.
 * <p>
 * As an interceptor the translation happens before the instance is visible, which closes that window and is what
 * lets the stored model be immutable.
 */
@RequiredArgsConstructor
public class MetadataTranslationInterceptor implements RegistrationInterceptor {

    private final MetadataTranslationService metadataTranslationService;
    private final MetadataDefaultsService metadataDefaultsService;

    @Override
    public ServiceInstance intercept(ServiceInstance instance) {
        Map<String, String> metadata = new LinkedHashMap<>(instance.metadata());
        String serviceId = EurekaUtils.getServiceIdFromInstanceId(instance.instanceId());

        metadataTranslationService.translateMetadata(serviceId, metadata);
        metadataDefaultsService.updateMetadata(serviceId, metadata);

        return instance.toBuilder().metadata(metadata).build();
    }

    @Override
    public int order() {
        // Before the conformance and allow-list checks, so both see the translated metadata rather than V1 keys.
        return 10;
    }

}
