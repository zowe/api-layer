/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.registry.InstanceRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.constants.CoreService;
import reactor.core.publisher.Flux;

import java.util.*;

@Configuration
public class ModulithConfig {

    @Value("${server.ssl.enabled:true}")
    private boolean https;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    @Value("${apiml.service.ipAddress:127.0.0.1}")
    private String ipAddress;

    @Value("${apiml.service.port:10010}")
    private int port;

    private final Map<String, InstanceInfo> localInstances = new HashMap<>();

    private InstanceInfo getInstanceInfo(String serviceId) {
        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(String.format("%s:%s:%d", hostname, serviceId, port))
            .setAppName(serviceId)
            //.setAppNameForDeser(String appName)
            //.setAppGroupName(String appGroupName)
            //.setAppGroupNameForDeser(String appGroupName)
            .setHostName(hostname)
            .setStatus(InstanceInfo.InstanceStatus.UP)
            //.setOverriddenStatus(InstanceStatus status)
            .setIPAddr(ipAddress)
            .setPort(port)
            .setSecurePort(port)
            .enablePort(InstanceInfo.PortType.SECURE, https)
            .enablePort(InstanceInfo.PortType.UNSECURE, !https)
            //.setHomePageUrl(String relativeUrl, String explicitUrl)
            //.setHomePageUrlForDeser(String homePageUrl)
            //.setStatusPageUrl(String relativeUrl, String explicitUrl)
            //.setStatusPageUrlForDeser(String statusPageUrl)
            //.setHealthCheckUrls(String relativeUrl, String explicitUrl, String secureExplicitUrl)
            //.setHealthCheckUrlsForDeser(String healthCheckUrl, String secureHealthCheckUrl)
            .setVIPAddress(serviceId)
            //.setVIPAddressDeser(String vipAddress)
            //.setSecureVIPAddress(final String secureVIPAddress)
            //.setSecureVIPAddressDeser(String secureVIPAddress)
            //.setDataCenterInfo(DataCenterInfo datacenter)
            //.setLeaseInfo(LeaseInfo info)
            //.add(String key, String val)
            //.setMetadata(Map<String, String> mt)
            //.setASGName(String asgName)
            //.setIsCoordinatingDiscoveryServer(boolean isCoordinatingDiscoveryServer)
            .setLastUpdatedTimestamp(System.currentTimeMillis())
            //.setLastDirtyTimestamp(long lastDirtyTimestamp)
            //.setActionType(ActionType actionType)
            //.setNamespace(String namespace)
            .build();
    }

    @PostConstruct
    void createLocalInstances() {
        localInstances.put(CoreService.GATEWAY.getServiceId(), getInstanceInfo(CoreService.GATEWAY.getServiceId()));
        localInstances.put(CoreService.DISCOVERY.getServiceId(), getInstanceInfo(CoreService.DISCOVERY.getServiceId()));
        localInstances.put(CoreService.ZAAS.getServiceId(), getInstanceInfo(CoreService.ZAAS.getServiceId()));
    }

    @Bean
    public ReactiveDiscoveryClient getLocalReactiveDiscoveryClient() {
        return new ReactiveDiscoveryClient() {
            @Override
            public String description() {
                return "Reactive discovery client of local instances";
            }

            @Override
            public Flux<ServiceInstance> getInstances(String serviceId) {
                var instanceInfo = localInstances.get(serviceId);
                if (instanceInfo == null) {
                    return Flux.empty();
                }
                return Flux.just(new EurekaServiceInstance(instanceInfo));
            }

            @Override
            public Flux<String> getServices() {
                return Flux.fromIterable(localInstances.keySet());
            }
        };
    }

    @Bean
    public DiscoveryClient getLocalDiscoveryClient() {
        return new DiscoveryClient() {
            @Override
            public String description() {
                return "Discovery client of local instances";
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {
                var instanceInfo = localInstances.get(serviceId);
                if (instanceInfo == null) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(new EurekaServiceInstance(instanceInfo));
            }

            @Override
            public List<String> getServices() {
                return new ArrayList<>(localInstances.keySet());
            }
        };
    }

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        InstanceRegistry instanceRegistry = event.getApplicationContext().getBean(InstanceRegistry.class);
        for (Map.Entry<String, InstanceInfo> entry : localInstances.entrySet()) {
            instanceRegistry.register(getInstanceInfo(entry.getKey()), Integer.MAX_VALUE, CoreService.GATEWAY.getServiceId().equals(entry.getKey()));
        }
    }

    @Bean
    public MessageService messageService() {
        MessageService messageService = YamlMessageServiceInstance.getInstance();
        messageService.loadMessages("/utility-log-messages.yml");
        messageService.loadMessages("/common-log-messages.yml");
        messageService.loadMessages("/security-common-log-messages.yml");

        messageService.loadMessages("/gateway-log-messages.yml");
        messageService.loadMessages("/zaas-log-messages.yml");
        return messageService;
    }

}
