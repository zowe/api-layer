/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.staticdef;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.InstanceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.discovery.ApimlInstanceRegistry;
import org.zowe.apiml.discovery.EurekaRegistryAvailableListener;
import org.zowe.apiml.discovery.metadata.MetadataDefaultsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Responsible for registration of statically defined APIs into Eureka and updating their status.
 *
 * The service called by {@link EurekaRegistryAvailableListener} that calls method {@link #registerServices()}.
 */
@Slf4j
@Component
public class StaticServicesRegistrationService {
    @Value("${apiml.discovery.staticApiDefinitionsDirectories:#{null}}")
    private String staticApiDefinitionsDirectories;

    private final ServiceDefinitionProcessor serviceDefinitionProcessor;
    private final MetadataDefaultsService metadataDefaultsService;

    private final List<InstanceInfo> staticInstances = new CopyOnWriteArrayList<>();

    @Autowired
    public StaticServicesRegistrationService(ServiceDefinitionProcessor serviceDefinitionProcessor, MetadataDefaultsService metadataDefaultsService) {
        this.serviceDefinitionProcessor = serviceDefinitionProcessor;
        this.metadataDefaultsService = metadataDefaultsService;
    }

    /**
     * Lists information about registered static service instances.
     */
    public List<InstanceInfo> getStaticInstances() {
        return staticInstances;
    }

    /**
     * Registers all statically defined APIs in locations specified by configuration.
     */
    public void registerServices() {
        registerServices(staticApiDefinitionsDirectories);
    }

    /**
     * Reloads all statically defined APIs in locations specified by configuration
     * by reading the definitions again.
     */
    public synchronized StaticRegistrationResult reloadServices() {
        List<InstanceInfo> oldStaticInstances = new ArrayList<>(staticInstances);

        staticInstances.clear();
        StaticRegistrationResult result = registerServices(staticApiDefinitionsDirectories);

        InstanceRegistry registry = getRegistry();
        for (InstanceInfo info : oldStaticInstances) {
            if (!result.getRegisteredServices().contains(info.getInstanceId())) {
                log.info("Instance {} is not defined in the new static API definitions. It will be removed", info.getInstanceId());
                registry.cancel(info.getAppName(), info.getId(), false);
            }
        }

        return result;
    }

    /**
     * Registers all statically defined APIs in a directory.
     */
    StaticRegistrationResult registerServices(String staticApiDefinitionsDirectories) {
        var registry = getRegistry();
        StaticRegistrationResult result = serviceDefinitionProcessor.findStaticServicesData(staticApiDefinitionsDirectories);

        // at first register service additional data, because static could be also updated
        final Map<String, ServiceOverrideData> additionalServiceMetadata = result.getAdditionalServiceMetadata();
        metadataDefaultsService.setAdditionalServiceMetadata(additionalServiceMetadata);

        // register static services
        for (InstanceInfo instanceInfo : result.getInstances()) {
            result.getRegisteredServices().add(instanceInfo.getInstanceId());
            staticInstances.add(instanceInfo);
            registry.registerStatically(instanceInfo, false);
        }

        return result;
    }

    private ApimlInstanceRegistry getRegistry() {
        return (ApimlInstanceRegistry) getServerContext().getRegistry();
    }

    private EurekaServerContext getServerContext() {
        return EurekaServerContextHolder.getInstance().getServerContext();
    }
}
