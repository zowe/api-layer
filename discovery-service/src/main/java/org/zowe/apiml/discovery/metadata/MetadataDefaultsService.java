/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discovery.metadata;

import org.zowe.apiml.discovery.staticdef.ServiceOverrideData;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

/**
 * This bean will correct metadata of service. It is helpful, if service does not support all new values. In this case
 * it is possible to define values in local static definition and metadata will be updated on registration.
 *
 * It could be also use to redefine those meta data.
 */
@Service
public class MetadataDefaultsService {

    /**
     * collect default values for
     */
    private Map<String, ServiceOverrideData> additionalServiceMetadata = Collections.emptyMap();

    public void updateMetadata(String serviceId, Map<String, String> metadata) {
        final ServiceOverrideData sod = additionalServiceMetadata.get(serviceId);

        if (sod != null) {
            update(sod, metadata);
        }
    }

    private void update(ServiceOverrideData sod, Map<String, String> metadata) {
        switch (sod.getMode()) {
            case FORCE_UPDATE:
                metadata.putAll(sod.getMetadata());
                break;
            case UPDATE: default:
                for (final Map.Entry<String, String> entry : sod.getMetadata().entrySet()) {
                    if (metadata.containsKey(entry.getKey())) continue;
                    metadata.put(entry.getKey(), entry.getValue());
                }
                break;
        }
    }

    public void setAdditionalServiceMetadata(Map<String, ServiceOverrideData> additionalServiceMetadata) {
        this.additionalServiceMetadata = Collections.unmodifiableMap(additionalServiceMetadata);
    }

}
