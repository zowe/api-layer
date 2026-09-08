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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.discovery.metadata.MetadataDefaultsService;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.discovery.ServiceOverrideData;
import org.zowe.apiml.product.discovery.StaticRegistrationResult;
import org.zowe.apiml.product.discovery.StaticServicesRegistration;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registers statically defined APIs into the registry and keeps their status current.
 * <p>
 * Driven by {@link org.zowe.apiml.discovery.registry.RegistryLifecycleListener}, which calls
 * {@link #registerServices()} once the registry reports itself available - the same trigger point the
 * Eureka-based implementation used, just without reaching through a static server-context holder.
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
    private final ServiceRegistry registry;

    private final List<ServiceInstance> staticInstances = new CopyOnWriteArrayList<>();

    public StaticServicesRegistrationService(
        ServiceDefinitionProcessor serviceDefinitionProcessor,
        MetadataDefaultsService metadataDefaultsService,
        ServiceRegistry registry
    ) {
        this.serviceDefinitionProcessor = serviceDefinitionProcessor;
        this.metadataDefaultsService = metadataDefaultsService;
        this.registry = registry;
    }

    /**
     * Lists information about registered static service instances.
     */
    public List<ServiceInstance> getStaticInstances() {
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

    String getAllMessages(Throwable throwable) {
        Throwable lastThrowable = null;
        var messages = new StringBuilder();
        while ((throwable != null) && (lastThrowable != throwable)) {
            if (!messages.isEmpty()) {
                messages.append(": ");
            }

            messages.append(throwable.getMessage());

            lastThrowable = throwable;
            throwable = throwable.getCause();
        }

        return messages.toString();
    }

    /**
     * Reloads all statically defined APIs in locations specified by configuration
     * by reading the definitions again.
     */
    public synchronized StaticRegistrationResult reloadServices() {
        List<ServiceInstance> oldStaticInstances = new ArrayList<>(staticInstances);

        staticInstances.clear();
        StaticRegistrationResult result = registerServices(staticApiDefinitionsDirectories);

        for (ServiceInstance info : oldStaticInstances) {
            if (!result.getRegisteredServices().contains(info.instanceId())) {
                log.info("Instance {} is not defined in the new static API definitions. It will be removed", info.instanceId());
                try {
                    registry.cancel(info.appName(), info.instanceId(), false);
                } catch (Exception e) {
                    final Message msg = apimlLog.log("org.zowe.apiml.discovery.staticDefinitionRegistration", staticApiDefinitionsDirectories, getAllMessages(e));
                    result.getErrors().add(msg);
                }
            }
        }

        return result;
    }

    /**
     * Applies the configured service overrides to a static definition.
     * <p>
     * Done here rather than relying on the registration interceptor because the result of this method is what
     * {@link #getStaticInstances()} publishes, and the API Catalog reads it. Under Eureka the equivalent happened
     * in a post-registration listener that mutated the instance's metadata map in place, so the list built here
     * happened to observe the change through the shared mutable object. The model is immutable now, so the
     * override has to be applied to the instance that is actually stored and returned.
     * <p>
     * The interceptor still runs on the way into the registry; for a static definition it is idempotent, since
     * the processor already emits current-version metadata.
     */
    private ServiceInstance applyServiceOverrides(ServiceInstance definition) {
        var metadata = new java.util.LinkedHashMap<>(definition.metadata());
        var serviceId = org.zowe.apiml.util.EurekaUtils.getServiceIdFromInstanceId(definition.instanceId());
        metadataDefaultsService.updateMetadata(serviceId, metadata);
        return definition.toBuilder().metadata(metadata).build();
    }

    void register(StaticRegistrationResult result, ServiceInstance instanceInfo) {
        try {
            // STATIC gives it a permanent lease and keeps it out of the renew-threshold accounting; the Eureka
            // implementation needed registerStatically() plus a ThreadLocal correction to achieve the same.
            registry.register(instanceInfo, RegistrationKind.STATIC);
        } catch (Exception e) {
            final Message msg = apimlLog.log("org.zowe.apiml.discovery.staticDefinitionRegistration", staticApiDefinitionsDirectories, getAllMessages(e));
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
            for (ServiceInstance definition : result.getInstances()) {
                var instanceInfo = applyServiceOverrides(definition);
                result.getRegisteredServices().add(instanceInfo.instanceId());
                staticInstances.add(instanceInfo);
                register(result, instanceInfo);
            }
        } catch (Exception e) {
            final Message msg = apimlLog.log("org.zowe.apiml.discovery.staticDefinitionUnexpectedError", staticApiDefinitionsDirectories, e.getMessage());
            result.getErrors().add(msg);
        }

        return result;
    }
}
