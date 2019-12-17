/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discovery.staticdef;

import com.ca.mfaas.discovery.EurekaRegistryAvailableListener;
import com.ca.mfaas.discovery.metadata.MetadataDefaultsService;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
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

    private final Timer renewalTimer = new Timer();

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
        startRenewalTimer();
    }

    private void startRenewalTimer() {
        renewalTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                renewInstances();
            }
        }, LeaseInfo.DEFAULT_LEASE_RENEWAL_INTERVAL * 1000L, LeaseInfo.DEFAULT_LEASE_RENEWAL_INTERVAL * 1000L);
    }

    synchronized void renewInstances() {
        log.debug("Renewing static instances");
        PeerAwareInstanceRegistry registry = getRegistry();
        for (InstanceInfo instance : getStaticInstances()) {
            registry.renew(instance.getAppName(), instance.getId(), false);
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

        PeerAwareInstanceRegistry registry = getRegistry();
        for (InstanceInfo info: oldStaticInstances) {
            if (!result.getRegistredServices().contains(info.getInstanceId())) {
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
        PeerAwareInstanceRegistry registry = getRegistry();
        StaticRegistrationResult result = serviceDefinitionProcessor.findStaticServicesData(staticApiDefinitionsDirectories);

        // at first register service additional data, becase static could be also updated
        final Map<String, ServiceOverrideData> additionalServiceMetadata = result.getAdditionalServiceMetadata();
        metadataDefaultsService.setAdditionalServiceMetadata(additionalServiceMetadata);

        // register static services
        for (InstanceInfo instanceInfo : result.getInstances()) {
            result.getRegistredServices().add(instanceInfo.getInstanceId());
            staticInstances.add(instanceInfo);
            registry.register(instanceInfo, false);
        }

        return result;
    }

    private PeerAwareInstanceRegistry getRegistry() {
        return getServerContext().getRegistry();
    }

    private EurekaServerContext getServerContext() {
        return EurekaServerContextHolder.getInstance().getServerContext();
    }
}
