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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.discovery.ApimlInstanceRegistry;
import org.zowe.apiml.discovery.EurekaRegistryAvailableListener;
import org.zowe.apiml.discovery.metadata.MetadataDefaultsService;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.discovery.ServiceOverrideData;
import org.zowe.apiml.product.discovery.StaticRegistrationResult;
import org.zowe.apiml.product.discovery.StaticServicesRegistration;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

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
public class StaticServicesRegistrationService implements StaticServicesRegistration {

    @Value("${apiml.discovery.staticApiDefinitionsDirectories:#{null}}")
    private String staticApiDefinitionsDirectories;

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    private final ServiceDefinitionProcessor serviceDefinitionProcessor;
    private final MetadataDefaultsService metadataDefaultsService;

    private final List<InstanceInfo> staticInstances = new CopyOnWriteArrayList<>();

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
        try {
            var result = registerServices(staticApiDefinitionsDirectories);

            if (result.hasError()) {
                log.error("Loading static definition failed: {}", result);
            } else {
                log.debug("Loaded static definition ended with the result: {}", result);
            }
        } catch (Exception e) {
            log.error("Cannot load static definition of services", e);
        }
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
                try {
                    registry.cancel(info.getAppName(), info.getId(), false);
                } catch (Exception e) {
                    final Message msg = apimlLog.log("org.zowe.apiml.discovery.staticDefinitionRegistration", e.getMessage());
                    result.getErrors().add(msg);
                }
            }
        }

        return result;
    }

    void register(StaticRegistrationResult result, InstanceInfo instanceInfo) {
        try {
            var registry = getRegistry();
            registry.registerStatically(instanceInfo, false, false);
        } catch (Exception e) {
            final Message msg = apimlLog.log("org.zowe.apiml.discovery.staticDefinitionRegistration", e.getMessage());
            result.getErrors().add(msg);
        }
    }

    /**
     * Registers all statically defined APIs in a directory.
     */
    StaticRegistrationResult registerServices(String staticApiDefinitionsDirectories) {
        StaticRegistrationResult result = new StaticRegistrationResult();

        try {
            result = serviceDefinitionProcessor.findStaticServicesData(staticApiDefinitionsDirectories);

            // at first register service additional data, because static could be also updated
            final Map<String, ServiceOverrideData> additionalServiceMetadata = result.getAdditionalServiceMetadata();
            metadataDefaultsService.setAdditionalServiceMetadata(additionalServiceMetadata);

            // register static services
            for (InstanceInfo instanceInfo : result.getInstances()) {
                result.getRegisteredServices().add(instanceInfo.getInstanceId());
                staticInstances.add(instanceInfo);
                register(result, instanceInfo);
            }
        } catch (Exception e) {
            final Message msg = apimlLog.log("org.zowe.apiml.discovery.staticDefinitionUnexpectedError", e.getMessage());
            result.getErrors().add(msg);
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
