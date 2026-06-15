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
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.lease.Lease;
import com.netflix.eureka.registry.AbstractInstanceRegistry;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import com.netflix.eureka.resources.ServerCodecs;
import com.netflix.eureka.transport.EurekaServerHttpClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistry;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistryProperties;
import org.springframework.context.ApplicationContext;
import org.zowe.apiml.discovery.config.EurekaConfig;
import org.zowe.apiml.discovery.metadata.MetadataFilterService;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.util.EurekaUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * This implementation of instance registry is solving known problem in Eureka. Discovery service notify about change
 * in services before it does it. From this reason listener can try to use services before they are really registered.
 * <p>
 * At least implementation with reflection of register and cancel should be removed after finish task in Eureka:
 * <p>
 * #2659 Race condition with registration events in Eureka server
 * https://github.com/spring-cloud/spring-cloud-netflix/issues/2659
 */
@Slf4j
public class ApimlInstanceRegistry extends InstanceRegistry {

    private static final String EXCEPTION_MESSAGE = "Implementation of InstanceRegistry changed, please verify fix of order sending events";

    private MethodHandle replicateToPeersMethodHandle;

    private final ApplicationContext appCntx;
    private final EurekaConfig.Tuple tuple;
    private final MetadataFilterService metadataFilterService;

    private ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>> registry;
    private Set<String> staticRegistrationIds = Collections.synchronizedSet(new HashSet<>());

    private static final ThreadLocal<Integer> RENEW_CORRECTION = new ThreadLocal<>();

    public ApimlInstanceRegistry(
        EurekaServerConfig serverConfig,
        EurekaClientConfig clientConfig,
        ServerCodecs serverCodecs,
        EurekaClient eurekaClient,
        EurekaServerHttpClientFactory eurekaServerHttpClientFactory,
        InstanceRegistryProperties instanceRegistryProperties,
        ApplicationContext appCntx,
        EurekaConfig.Tuple tuple,
        MetadataFilterService metadataFilterService
    ) {

        super(serverConfig, clientConfig, serverCodecs, eurekaClient, eurekaServerHttpClientFactory,
            instanceRegistryProperties.getExpectedNumberOfClientsSendingRenews(),
            instanceRegistryProperties.getDefaultOpenForTrafficCount()
        );
        this.appCntx = appCntx;
        this.tuple = tuple;
        this.metadataFilterService = metadataFilterService;
        init();
    }

    /**
     * Prepare method handlers to overridden methods to reimplement methods in InstanceRegistry, which contains a race
     * condition problem. Handlers are faster than reflection, close to bytecode.
     */
    private void init() {
        try {
            Field registryField = AbstractInstanceRegistry.class.getDeclaredField("registry");
            registryField.setAccessible(true);
            this.registry = (ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>>) registryField.get(this);

            Method replicateToPeers = PeerAwareInstanceRegistryImpl.class.getDeclaredMethod("replicateToPeers", Action.class, String.class, String.class, InstanceInfo.class, InstanceStatus.class, boolean.class);
            replicateToPeers.setAccessible(true);
            replicateToPeersMethodHandle = MethodHandles.lookup().unreflect(replicateToPeers);
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE, e);
        }
    }

    @Override
    protected void updateRenewsPerMinThreshold() {
        Integer correction = RENEW_CORRECTION.get();
        if (correction != null) {
            synchronized (lock) {
                this.expectedNumberOfClientsSendingRenews += correction;
            }
            RENEW_CORRECTION.remove();
        }

        super.updateRenewsPerMinThreshold();
    }

    public void peerAwareHeartbeat(InstanceInfo instanceInfo) {
        try {
            replicateToPeersMethodHandle.invokeWithArguments(this, Action.Heartbeat, instanceInfo.getAppName(), instanceInfo.getId(), instanceInfo, null, false);
        } catch (Throwable e) {
            log.warn("Unexpected error occurred while replicateToPeers: {}", instanceInfo.getInstanceId(), e);
            throw new IllegalStateException(EXCEPTION_MESSAGE, e);
        }
    }

    /**
     * Register a service statically
     *
     * @param instanceInfo InstanceInfo of the registered instance
     * @param isReplication Whether the registration information source is a replication event
     * @param peerReplicate Whether to peer replicate the newly registered instance
     */
    public void registerStatically(InstanceInfo instanceInfo, boolean isReplication, boolean peerReplicate) {
        // the maximum lease duration time (Eureka bug: overflow of int during conversion to ms)
        int leaseDuration = Integer.MAX_VALUE / 1000;

        try {
            // temporary register (do not increase count of service to avoid threshold)
            RENEW_CORRECTION.set(-1);
            register(instanceInfo, leaseDuration, isReplication);
            if (peerReplicate) {
                replicateToPeersMethodHandle.invokeWithArguments(this, Action.Register, instanceInfo.getAppName(), instanceInfo.getId(), instanceInfo, null, isReplication);
            }
        } catch (Throwable e) {
            throw new IllegalStateException(EXCEPTION_MESSAGE, e);
        } finally {
            RENEW_CORRECTION.remove();
        }

        // register lease plan to never expired
        Map<String, Lease<InstanceInfo>> leaseMap = registry.get(instanceInfo.getAppName());
        leaseMap.put(instanceInfo.getInstanceId(), new Lease<>(instanceInfo, leaseDuration) {
            @Override
            public boolean isExpired() {
                return false;
            }

            @Override
            public boolean isExpired(long additionalLeaseMs) {
                return false;
            }
        });

        // remember instanceId to avoid from threashold
        staticRegistrationIds.add(instanceInfo.getInstanceId());
    }

    /**
     * Does not do peer replica
     */
    @Override
    public void register(InstanceInfo info, int leaseDuration, boolean isReplication) {
        validateInstanceInfo(info);
        info = changeServiceId(info);

        super.register(info, leaseDuration, isReplication);
    }

    @Override
    public void register(InstanceInfo info, final boolean isReplication) {
        validateInstanceInfo(info);
        info = changeServiceId(info);

        super.register(info, isReplication);
    }

    /**
     * Validates that the service identifiers in the {@link InstanceInfo} are conformant and mutually consistent.
     * The appName must not be null or empty. Both must comply with RFC 952 and RFC 1123.
     * Only lowercase letters, digits, and hyphens allowed, must not start or end with a hyphen, and must not exceed 63 characters.
     * Unfortunately the java enabler converts the appName to uppercase when sending the registration request.
     * Therefore, the validation is case-insensitive.
     * The instanceId must follow the format 'hostname:serviceId:port'.
     * The serviceId extracted from the instanceId must match the appName, the check is again case-insensitive for the reason
     * described above. For backwards compatibility the validation prints warnings only for non-conformant values.
     * @param info the instance info
     */
    private void validateInstanceInfo(InstanceInfo info) {
        metadataFilterService.verifyAllowedDomains(info);

        String instanceId = info.getInstanceId();
        String appName = StringUtils.lowerCase(info.getAppName());

        try {
            EurekaUtils.validateServiceId(appName);
        } catch (MetadataValidationException e) {
            log.warn("Conformance criteria violation in serviceId or app in instanceId for: '{}'. Cause: {}" , info.getInstanceId(), e.getMessage());
        }

        String serviceId = EurekaUtils.getServiceIdFromInstanceId(instanceId);
        try {
            EurekaUtils.validateServiceId(serviceId);
        } catch (MetadataValidationException e) {
            log.warn("Conformance criteria violation in serviceId or instanceId for '{}'. Cause: {}", info.getInstanceId(), e.getMessage());
        }

        if (!Objects.equals(appName, StringUtils.lowerCase(serviceId))) {
            log.warn(
                "Inconsistent service identity: instanceId contains serviceId '{}' but appName='{}'", serviceId, appName);
        }
    }

    @Override
    public long getNumOfRenewsInLastMin() {
        // to simulate APIML, it is not sending a heartbeat anymore
        return super.getNumOfRenewsInLastMin() + 2;
    }

    @Override
    public boolean isRegisterable(InstanceInfo instanceInfo) {
        if (staticRegistrationIds.contains(instanceInfo.getInstanceId())) {
            return false;
        }
        return super.isRegisterable(instanceInfo);
    }

    @Override
    public boolean cancel(String appName, String serverId, boolean isReplication) {
        try {
            String[] updatedValues = replaceValues(appName, serverId);

            if (staticRegistrationIds.removeAll(Optional.ofNullable(registry.get(appName)).orElse(Collections.emptyMap()).keySet())) {
                // do not change count of instances if it was registered statically
                RENEW_CORRECTION.set(1);
            }
            return super.cancel(updatedValues[0], updatedValues[1], isReplication);
        } finally {
            RENEW_CORRECTION.remove();
        }
    }

    @Override
    public boolean renew(String appName, String serverId, boolean isReplication) {
        String[] updatedValues = replaceValues(appName, serverId);
        return super.renew(updatedValues[0], updatedValues[1], isReplication);
    }

    @Override
    public boolean statusUpdate(String appName, String instanceId, InstanceInfo.InstanceStatus newStatus, String lastDirtyTimestamp, boolean isReplication) {
        String[] updatedValues = replaceValues(appName, instanceId);
        boolean isUpdated = super.statusUpdate(updatedValues[0], updatedValues[1], newStatus, lastDirtyTimestamp, isReplication);
        this.appCntx.publishEvent(new EurekaStatusUpdateEvent(this, appName, instanceId));
        return isUpdated;
    }

    private String[] replaceValues(String appName, String instanceId) {
        if (tuple.isValid()) {
            String appNameRegex = "(?i)^" + tuple.getOldPrefix();
            String instanceIdRegex = "(?i):" + tuple.getOldPrefix();
            String targetValue = tuple.getNewPrefix();
            if (targetValue.contains("*")) {
                targetValue = targetValue.replace("*", "");
            }
            appName = appName.replaceAll(appNameRegex, targetValue).toUpperCase();
            if (instanceId.contains(":")) {
                instanceId = instanceId.replaceAll(instanceIdRegex, ":" + targetValue);
            } else {
                instanceId = instanceId.replaceAll(appNameRegex, targetValue);
            }
        }
        return new String[]{appName,instanceId};
    }

    /**
     * Change the service ID prefix according to the mapper before the service registers to Eureka.
     * @param info the instance info
     * @return instance info with the modified service ID
     */
    protected InstanceInfo changeServiceId(final InstanceInfo info) {
        if (tuple.isValid()) {
            String servicePrefix = tuple.getOldPrefix();
            if (!servicePrefix.contains("*")) {
                servicePrefix = servicePrefix + "*";
            }
            String instanceId = info.getInstanceId();
            String appName = info.getAppName();
            Pattern p = Pattern.compile("(?i)^" + servicePrefix);
            if (p.matcher(appName).find()) {
                String[] updatedValues = replaceValues(appName, instanceId);
                log.debug("The instance ID of {} service has been changed to {}.", info.getAppName(), updatedValues[1]);
                return new InstanceInfo.Builder(info)
                    .setInstanceId(updatedValues[1])
                    .setAppGroupName(updatedValues[0])
                    .setAppName(updatedValues[0])
                    .setVIPAddress(updatedValues[0].toLowerCase())
                    .build();
            }
            return info;
        }
        return info;
    }

}
