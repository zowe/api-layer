/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.util;

import com.netflix.appinfo.InstanceInfo;
import lombok.experimental.UtilityClass;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;

import java.util.HashMap;
import java.util.Map;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

@UtilityClass
public class ServicesBuilder {

    private int id = 0;

    public ServiceInstance createInstance(String serviceId,
                                             InstanceInfo.InstanceStatus status,
                                             Map<String, String> metadata) {
        return new EurekaServiceInstance(InstanceInfo.Builder.newBuilder()
                .setInstanceId(serviceId + (id++))
                .setAppName(serviceId)
                .setStatus(status)
                .setHostName("localhost")
                .setHomePageUrl(null, "https://localhost:8080/")
                .setVIPAddress(serviceId)
                .setMetadata(metadata)
                .build());
    }

    public ServiceInstance createInstance(String serviceId, String catalogId, Map.Entry<String, String>...otherMetadata) {
        return createInstance(serviceId, catalogId, InstanceInfo.InstanceStatus.UP, otherMetadata);
    }

    public ServiceInstance createInstance(
            String serviceId, String catalogId, InstanceInfo.InstanceStatus status,
            Map.Entry<String, String>...otherMetadata
    ) {
        return createInstance(
                serviceId, catalogId, "Title", "Description", "1.0.0", status,
                otherMetadata);
    }

    public ServiceInstance createInstance(String serviceId,
                                   String catalogId,
                                   String catalogTitle,
                                   String catalogDescription,
                                   String catalogVersion,
                                   InstanceInfo.InstanceStatus status,
                                   Map.Entry<String, String>...otherMetadata) {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(CATALOG_ID, catalogId);
        metadata.put(CATALOG_TITLE, catalogTitle);
        metadata.put(CATALOG_DESCRIPTION, catalogDescription);
        metadata.put(CATALOG_VERSION, catalogVersion);
        for (Map.Entry<String, String> entry : otherMetadata) {
            metadata.put(entry.getKey(), entry.getValue());
        }

        return createInstance(serviceId, status, metadata);
    }

}
