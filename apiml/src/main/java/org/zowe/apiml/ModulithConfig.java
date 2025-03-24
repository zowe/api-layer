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

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.zowe.apiml.discovery.ApimlInstanceRegistry;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.constants.CoreService;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ModulithConfig {

    private final EurekaServerContext eurekaContext;
    private final Map<String, InstanceInfo> instances = new HashMap<>();;

    @Value("${server.ssl.enabled:true}")
    private boolean https;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    @Value("${apiml.service.ipAddress:127.0.0.1}")
    private String ipAddress;

    @Value("${apiml.service.port:10010}")
    private int port;

    private InstanceInfo getInstanceInfo(String serviceId) {
        // TODO: Does this support HA?
        var leaseInfo = LeaseInfo.Builder.newBuilder()
            .setDurationInSecs(Integer.MAX_VALUE)
            .setRegistrationTimestamp(System.currentTimeMillis())
            .setRenewalTimestamp(System.currentTimeMillis())
            .setRenewalIntervalInSecs(Integer.MAX_VALUE)
            .setServiceUpTimestamp(System.currentTimeMillis())
            .build();

        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(String.format("%s:%s:%d", hostname, serviceId, port))
            .setAppName(serviceId)
            .setHostName(hostname)
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .setIPAddr(ipAddress)
            .setPort(port)
            .setSecurePort(port)
            .enablePort(InstanceInfo.PortType.SECURE, https)
            .enablePort(InstanceInfo.PortType.UNSECURE, !https)
            .setVIPAddress(serviceId)
            .setDataCenterInfo(() -> DataCenterInfo.Name.MyOwn)
            .setLeaseInfo(leaseInfo)
            .setLastUpdatedTimestamp(System.currentTimeMillis())
            .build();
    }

    private ApimlInstanceRegistry getRegistry() {
        return Optional.ofNullable(EurekaServerContextHolder.getInstance())
            .map(EurekaServerContextHolder::getServerContext)
            .map(EurekaServerContext::getRegistry)
            .map(ApimlInstanceRegistry.class::cast)
            .orElse(null);
    }

    @PostConstruct
    void createLocalInstances() {
        instances.put(CoreService.GATEWAY.getServiceId(), getInstanceInfo(CoreService.GATEWAY.getServiceId()));
        instances.put(CoreService.DISCOVERY.getServiceId(), getInstanceInfo(CoreService.DISCOVERY.getServiceId()));
        EurekaServerContextHolder.initialize(eurekaContext);
    }

    @EventListener
    public void onApplicationEvent(EurekaRegistryAvailableEvent event) {
        log.error("EurekaRegistry available");
        ApimlInstanceRegistry registry = getRegistry();
        instances.entrySet()
            .stream()
            .forEach(entry -> registry.registerStatically(instances.get(entry.getKey()), CoreService.GATEWAY.getServiceId().equals(entry.getKey())));
    }

    @Bean
    public ReactiveDiscoveryClient registryReactiveDiscoveryClient(DiscoveryClient registryDiscoveryClient) {
        return new ReactiveDiscoveryClient() {
            @Override
            public String description() {
                return "Reactive discovery client of local instances";
            }

            @Override
            public Flux<ServiceInstance> getInstances(String serviceId) {
                return Flux.fromIterable(registryDiscoveryClient.getInstances(serviceId));
            }

            @Override
            public Flux<String> getServices() {
                return Flux.fromIterable(registryDiscoveryClient.getServices());
            }
        };
    }

    @Bean
    public DiscoveryClient registryDiscoveryClient() {
        return new DiscoveryClient() {
            @Override
            public String description() {
                return "Discovery client of local instances";
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {
                var registry = getRegistry();
                if (registry == null) {
                    return Collections.emptyList();
                }
                return Optional.ofNullable(registry.getApplication(StringUtils.upperCase(serviceId)))
                    .map(Application::getInstances)
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(EurekaServiceInstance::new)
                    .map(ServiceInstance.class::cast)
                    .toList();
            }

            @Override
            public List<String> getServices() {
                var registry = getRegistry();
                if (registry == null) {
                    return Collections.emptyList();
                }
                return registry.getApplications().getRegisteredApplications()
                    .stream()
                    .map(Application::getName)
                    .distinct()
                    .toList();
            }
        };
    }

    @Bean
    public MessageService messageService() {
        MessageService messageService = YamlMessageServiceInstance.getInstance();
        messageService.loadMessages("/utility-log-messages.yml");
        messageService.loadMessages("/common-log-messages.yml");

        messageService.loadMessages("/discovery-log-messages.yml");
        messageService.loadMessages("/gateway-log-messages.yml");

        messageService.loadMessages("/apiml-log-messages.yml");
        return messageService;
    }


}
