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
import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.TomcatHttpHandlerAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.context.ServletContextAware;
import org.zowe.apiml.config.ApplicationInfo;
import org.zowe.apiml.discovery.ApimlInstanceRegistry;
import org.zowe.apiml.filter.PreFluxFilter;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.zaas.security.service.JwtSecurity;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@EnableScheduling
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties
@Slf4j
public class ModulithConfig {

    private final ApplicationContext applicationContext;
    private final Map<String, InstanceInfo> instances = new HashMap<>();
    private final GatewayEurekaInstanceConfigBean eurekaInstanceGw;
    private final EurekaClientConfig eurekaConfig;
    private final CachingServiceEurekaInstanceConfigBean eurekaInstanceCaching;

    private final Timer timer = new Timer("PeerReplicated-StaticServices");

    @Value("${server.ssl.enabled:true}")
    private boolean https;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    @Value("${apiml.service.ipAddress:127.0.0.1}")
    private String ipAddress;

    @Value("${apiml.service.port:10010}")
    private int port;

    @Bean
    ApplicationInfo applicationInfo() {
        return ApplicationInfo.builder()
            .isModulith(true)
            .authServiceId(CoreService.GATEWAY.getServiceId()).build();
    }

    private InstanceInfo getInstanceInfo(String serviceId) {
        var leaseInfo = LeaseInfo.Builder.newBuilder()
                .setDurationInSecs(Integer.MAX_VALUE)
                .setRegistrationTimestamp(System.currentTimeMillis())
                .setRenewalTimestamp(System.currentTimeMillis())
                .setRenewalIntervalInSecs(Integer.MAX_VALUE)
                .setServiceUpTimestamp(System.currentTimeMillis())
                .build();

        var scheme = https ? "https" : "http";

        Map<String, String> metadata = new HashMap<>();
        switch (serviceId) {
            case "gateway":
                metadata = eurekaInstanceGw.getMetadataMap();
                metadata.put("management.port", "10010");
                break;
            case "cachingservice":
                metadata = eurekaInstanceCaching.getMetadataMap();
                break;
            default:
        }

        return InstanceInfo.Builder.newBuilder()
                .setInstanceId(String.format("%s:%s:%d", hostname, serviceId, port))
                .setAppName(serviceId)
                .setHostName(hostname)
                .setHomePageUrl(null, String.format("%s://%s:%d", scheme, hostname, port))
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
                .setMetadata(metadata)
                .setVIPAddress(serviceId)
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
        instances.put(CoreService.CACHING.getServiceId(), getInstanceInfo(CoreService.CACHING.getServiceId()));
        EurekaServerContextHolder.initialize(applicationContext.getBean(EurekaServerContext.class));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStart() {
        ApimlInstanceRegistry registry = getRegistry();
        instances.forEach((key, value) -> registry.registerStatically(instances.get(key), false, CoreService.GATEWAY.getServiceId().equalsIgnoreCase(key)));

        log.info("Initialize timer for static services peer-replicated heartbeats");

        // This timer calls Eureka registry's peerReplicate method to accumulate all heartbeats of statically-onboarded services once
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                registry.peerAwareHeartbeat(instances.get(CoreService.GATEWAY.getServiceId()));
            }

        }, eurekaConfig.getInstanceInfoReplicationIntervalSeconds() * 1000L, eurekaConfig.getInstanceInfoReplicationIntervalSeconds() * 1000L);

    }

    @Scheduled(initialDelay = 3000, fixedRate = 20_000) // TODO find better solution but DON'T JUST REMOVE!
    public void periodicJwtInit() {
        var jwtSec = applicationContext.getBean(JwtSecurity.class);
        if (!jwtSec.getZosmfListener().isZosmfReady()) {
            jwtSec.getZosmfListener().getZosmfRegisteredListener().onEvent(new CacheRefreshedEvent());
        }
    }


    @Bean
    ReactiveDiscoveryClient registryReactiveDiscoveryClient(DiscoveryClient registryDiscoveryClient) {
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
    RouteRefreshListener routeRefreshListener(ApplicationEventPublisher publisher) {
        return new RouteRefreshListener(publisher);
    }

    @Bean
    DiscoveryClient registryDiscoveryClient() {
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
    MessageService messageService() {
        MessageService messageService = YamlMessageServiceInstance.getInstance();
        messageService.loadMessages("/utility-log-messages.yml");
        messageService.loadMessages("/common-log-messages.yml");

        messageService.loadMessages("/discovery-log-messages.yml");
        messageService.loadMessages("/gateway-log-messages.yml");

        messageService.loadMessages("/zaas-log-messages.yml");

        messageService.loadMessages("/caching-log-messages.yml");
        return messageService;
    }

    @Bean
    @Primary
    TomcatReactiveWebServerFactory tomcatReactiveWebServerWithFiltersFactory(
            HttpHandler httpHandler,
            List<PreFluxFilter> preFluxFilters,
            List<ServletContextAware> servletContextAwareListeners) {

        return new TomcatReactiveWebServerFactory() {
            @Override
            protected void prepareContext(Host host, TomcatHttpHandlerAdapter servlet) {
                super.prepareContext(host, new ServletWithFilters(httpHandler, servlet, preFluxFilters));
            }

            @Override
            protected void configureContext(Context context) {
                servletContextAwareListeners.forEach(l -> l.setServletContext(context.getServletContext()));
                super.configureContext(context);
            }
        };
    }

    /**
     * Create a custom Tomcat connector with same customizations as the main
     * external (GW) connector to handle
     * "legacy" connections in v3 meant to go to Eureka / Discovery Service
     *
     * @param internalDiscoveryPort port that will handle legacy Discovery Service
     *                              connections
     * @return
     */
    @Bean
    WebServerFactoryCustomizer<TomcatReactiveWebServerFactory> internalPortCustomizer(
            @Value("${apiml.internal-discovery.port:10011}") int internalDiscoveryPort) {
        return factory -> {
            var connector = new Connector();

            try {
                Method method = TomcatReactiveWebServerFactory.class.getDeclaredMethod("customizeConnector",
                        Connector.class);
                method.setAccessible(true);
                method.invoke(factory, connector);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            connector.setPort(internalDiscoveryPort);

            factory.addAdditionalTomcatConnectors(connector);
        };

    }

    static class ServletWithFilters extends TomcatHttpHandlerAdapter {

        private final Servlet servlet;
        private final FilterChain filterChain;

        public ServletWithFilters(HttpHandler httpHandler, TomcatHttpHandlerAdapter servlet,
                Collection<? extends Filter> filters) {
            super(httpHandler);
            this.servlet = servlet;

            FilterChain chain = servlet::service;
            for (var filter : filters) {
                chain = createFilterChain(filter, chain);
            }
            this.filterChain = chain;
        }

        FilterChain createFilterChain(Filter filter, FilterChain filterChain) {
            return (request, response) -> filter.doFilter(request, response, filterChain);
        }

        @Override
        public void init(ServletConfig config) {
            try {
                servlet.init(config);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ServletConfig getServletConfig() {
            return servlet.getServletConfig();
        }

        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            this.filterChain.doFilter(req, res);
        }

        @Override
        public String getServletInfo() {
            return servlet.getServletInfo();
        }

        @Override
        public void destroy() {
            servlet.destroy();
        }

    }

}
