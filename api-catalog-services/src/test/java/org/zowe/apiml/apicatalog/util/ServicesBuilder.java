package org.zowe.apiml.apicatalog.util;

import java.util.HashMap;
import java.util.Map;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;

import org.zowe.apiml.apicatalog.services.cached.CachedProductFamilyService;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

public class ServicesBuilder {
    private int id = 0;
    private CachedProductFamilyService service;

    public InstanceInfo instance1;
    public InstanceInfo instance2;

    public ServicesBuilder(CachedProductFamilyService service) {
        this.service = service;

        instance1 = createInstance("service1", "demoapp");
        instance2 = createInstance("service2", "demoapp2");
    }

    public Application createApp(String serviceId, InstanceInfo...instanceInfos) {
        Application application = new Application(serviceId);
        for (InstanceInfo instanceInfo : instanceInfos) {
            application.addInstance(instanceInfo);
            service.saveContainerFromInstance(serviceId, instanceInfo);
        }
        return application;
    }

    public InstanceInfo createInstance(String serviceId,
                                             InstanceInfo.InstanceStatus status,
                                             Map<String, String> metadata) {
        return InstanceInfo.Builder.newBuilder()
                .setInstanceId(serviceId + (id++))
                .setAppName(serviceId)
                .setStatus(status)
                .setHostName("localhost")
                .setHomePageUrl(null, "https://localhost:8080/")
                .setVIPAddress(serviceId)
                .setMetadata(metadata)
                .build();
    }

    public InstanceInfo createInstance(String serviceId, String catalogId, Map.Entry<String, String>...otherMetadata) {
        return createInstance(serviceId, catalogId, InstanceInfo.InstanceStatus.UP, otherMetadata);
    }

    public InstanceInfo createInstance(
            String serviceId, String catalogId, InstanceInfo.InstanceStatus status,
            Map.Entry<String, String>...otherMetadata
    ) {
        return createInstance(
                serviceId, catalogId, "Title", "Description", "1.0.0", status,
                otherMetadata);
    }

    public InstanceInfo createInstance(
            String serviceId, String catalogId, String catalogVersion, String title,
            Map.Entry<String, String>...otherMetadata
    ) {
        return createInstance(
                serviceId, catalogId, title, "Description", catalogVersion, InstanceInfo.InstanceStatus.UP,
                otherMetadata);
    }

    public InstanceInfo createInstance(String serviceId,
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
