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
import com.netflix.appinfo.LeaseInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.context.ServletContextAware;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.constants.CoreService;
import reactor.core.publisher.Flux;

import java.util.*;

@Configuration
@RequiredArgsConstructor
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
    private final ApplicationContext applicationContext;

    private InstanceInfo getInstanceInfo(String serviceId) {
        var leaseInfo = LeaseInfo.Builder.newBuilder()
            .setDurationInSecs(Integer.MAX_VALUE)
            .setEvictionTimestamp(Long.MAX_VALUE)
            .setRegistrationTimestamp(System.currentTimeMillis())
            .setRenewalTimestamp(System.currentTimeMillis())
            .setRenewalIntervalInSecs(Integer.MAX_VALUE)
            .setServiceUpTimestamp(System.currentTimeMillis())
            .build();

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
            .setLeaseInfo(leaseInfo)
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

    private PeerAwareInstanceRegistry getRegistry() {
        return Optional.ofNullable(EurekaServerContextHolder.getInstance())
            .map(EurekaServerContextHolder::getServerContext)
            .map(EurekaServerContext::getRegistry)
            .orElse(null);
    }

    @PostConstruct
    void createLocalInstances() {
        localInstances.put(CoreService.GATEWAY.getServiceId(), getInstanceInfo(CoreService.GATEWAY.getServiceId()));
        localInstances.put(CoreService.DISCOVERY.getServiceId(), getInstanceInfo(CoreService.DISCOVERY.getServiceId()));
        localInstances.put(CoreService.ZAAS.getServiceId(), getInstanceInfo(CoreService.ZAAS.getServiceId()));

        EurekaServerContextHolder.initialize(applicationContext.getBean(EurekaServerContext.class));
    }

    @EventListener
    public void onApplicationEvent(EurekaRegistryAvailableEvent event) {
        var registry = getRegistry();
        for (Map.Entry<String, InstanceInfo> entry : localInstances.entrySet()) {
            registry.register(getInstanceInfo(entry.getKey()), Integer.MAX_VALUE, CoreService.GATEWAY.getServiceId().equals(entry.getKey()));
        }
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
        messageService.loadMessages("/security-common-log-messages.yml");

        messageService.loadMessages("/gateway-log-messages.yml");
        messageService.loadMessages("/zaas-log-messages.yml");
        return messageService;
    }

    @Bean
    public TomcatContextCustomizer servletContextPropagator(List<ServletContextAware> listeners) {
        return context -> {
            var sc = context.getServletContext();
            listeners.forEach(l -> l.setServletContext(sc));
        };
    }

}
