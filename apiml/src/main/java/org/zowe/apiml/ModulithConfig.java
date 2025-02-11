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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.TomcatHttpHandlerAdapter;
import org.springframework.web.context.ServletContextAware;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.constants.CoreService;
import reactor.core.publisher.Flux;

import java.io.IOException;
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
            //.setEvictionTimestamp(Long.MAX_VALUE)
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

    @Bean
    public WebServerFactoryCustomizer<TomcatReactiveWebServerFactory> internalPortCustomizer(
        @Value("${apiml.service.scheme:https}") String scheme,
        @Value("${apiml.internal.port:8888}") int internalPort
    ) throws LifecycleException {
        return factory -> {
            var connector = new Connector();
            connector.setPort(internalPort);
            connector.setScheme(scheme);
            connector.setSecure("https".equals(scheme));

            var sslHostConfig = new SSLHostConfig();
            // TODO: correct the configuration to respect Spring config file
            connector.addSslHostConfig(sslHostConfig);

            factory.getAdditionalTomcatConnectors().add(connector);
        };
    }

    @Bean
    @Primary
    public TomcatReactiveWebServerFactory tomcatReactiveWebServerWithFiltersFactory(
        @Value("${apiml.service.port:10010}") int externalPort,
        MessageService messageService,
        HttpHandler httpHandler
    ) throws JsonProcessingException {

        String error404Message = new ObjectMapper().writeValueAsString(
            messageService.createMessage("org.zowe.apiml.common.notFound").mapToView()
        );

        var externalPortBlockingFilter = new Filter() {

            boolean isBlocked(HttpServletRequest request) {
                if (request.getServerPort() != externalPort) {
                    return false;
                }

                return
                    StringUtils.equals(request.getRequestURI(), "/eureka") ||
                    StringUtils.startsWith(request.getRequestURI(), "/eureka/");
            }

            @Override
            public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
                HttpServletRequest request = (HttpServletRequest) req;
                HttpServletResponse response = (HttpServletResponse) res;

                if (isBlocked(request)) {
                    response.getOutputStream().print(error404Message);
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    chain.doFilter(request, response);
                }
            }
        };

        return new TomcatReactiveWebServerFactory() {
            @Override
            protected void prepareContext(Host host, TomcatHttpHandlerAdapter servlet) {
                super.prepareContext(host, new ServletWithFilters(httpHandler, servlet, externalPortBlockingFilter));
            }
        };
    }

    static class ServletWithFilters extends TomcatHttpHandlerAdapter {

        private final Servlet servlet;
        private final FilterChain filterChain;

        public ServletWithFilters(HttpHandler httpHandler, TomcatHttpHandlerAdapter servlet, Filter...filters) {
            super(httpHandler);
            this.servlet = servlet;

            FilterChain filterChain = servlet::service;
            for (var filter : filters) {
                filterChain = createFilterChain(filter, filterChain);
            }
            this.filterChain = filterChain;
        }

        FilterChain createFilterChain(Filter filter, FilterChain filterChain) {
            return (request, response) -> {
                filter.doFilter(request, response, filterChain);
            };
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
