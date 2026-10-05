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

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.TomcatHttpHandlerAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.context.ServletContextAware;
import org.zowe.apiml.apicatalog.ApiCatalogServiceAvailableEvent;
import org.zowe.apiml.config.ApplicationInfo;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.SelfRegistration;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.client.spring.RegistryFetchProperties;
import org.zowe.apiml.registry.replication.PeerReplicator;
import org.zowe.apiml.filter.PreFluxFilter;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.services.BasicInfoService;
import org.zowe.apiml.zaas.security.login.Providers;
import org.zowe.apiml.zaas.security.service.JwtSecurity;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;

@EnableScheduling
@EnableRetry
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties
@DependsOn(value = {"gatewayHealthIndicator"})
@Slf4j
@OpenAPIDefinition(
    security = {
        @SecurityRequirement(name = "LoginBasicAuth"),
        @SecurityRequirement(name = "ClientCert")
    },
    info = @Info(title = "API Mediation Layer", description = "The API Mediation Layer REST API.")
)
@SecurityScheme(
    name = "LoginBasicAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "basic"
)
@SecurityScheme(
    type = SecuritySchemeType.MUTUALTLS,
    name = "ClientCert",
    description = "Client certificate X509"
)
public class ModulithConfig {

    private final ApplicationContext applicationContext;
    private final Map<String, org.zowe.apiml.registry.model.ServiceInstance> instances = new HashMap<>();
    private final GatewayEurekaInstanceConfigBean eurekaInstanceGw;
    private final CatalogEurekaInstanceConfigBean catalogEurekaInstanceConfigBean;
    private final RegistryFetchProperties registryConfig;
    private final CachingServiceEurekaInstanceConfigBean cachingServiceEurekaInstanceConfigBean;
    private final ApplicationEventPublisher eventPublisher;
    private final InetUtils inetUtils;

    private final Timer timer = new Timer("PeerReplicated-StaticServices");

    @Value("${server.ssl.enabled:true}")
    private boolean https;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    @Value("${eureka.instance.ipaddress:#{null}}")
    private String ipAddress;

    @Value("${apiml.service.port:10010}")
    private int gatewayPort;

    @Value("${apiml.internal-discovery.port:10011}")
    private int discoveryPort;

    @Value("${server.attlsServer.enabled:false}")
    private boolean isServerAttlsEnabled;

    @Value("${apiml.service.externalUrl}")
    private String externalUrl;

    @Bean
    ApplicationInfo applicationInfo() {
        return ApplicationInfo.builder()
            .isModulith(true)
            .authServiceId(CoreService.GATEWAY.getServiceId()).build();
    }

    @PostConstruct
    public void validateIpAddress() {
        if (StringUtils.isBlank(ipAddress)) {
            ipAddress = inetUtils.findFirstNonLoopbackAddress().getHostAddress();
            log.debug("Using resolved ip address {} for eureka client", ipAddress);
        }
    }

    private int getPort(String serviceId) {
        return Strings.CI.equals(serviceId, CoreService.DISCOVERY.getServiceId()) ? discoveryPort : gatewayPort;
    }

    private org.zowe.apiml.registry.model.ServiceInstance getInstanceInfo(String serviceId) {
        int port = getPort(serviceId);

        // PERMANENT: a core service inside the modulith does not heartbeat to a registry running in the same
        // JVM. The duration values are carried only for the wire representation peers receive.
        var leaseInfo = org.zowe.apiml.registry.model.Lease.builder()
            .kind(org.zowe.apiml.registry.model.Lease.Kind.PERMANENT)
            .durationSecs(90)
            .renewalIntervalSecs(30)
            .registrationTimestamp(System.currentTimeMillis())
            .lastRenewalTimestamp(System.currentTimeMillis())
            .serviceUpTimestamp(System.currentTimeMillis())
            .build();


        Map<String, String> metadata = switch (serviceId) {
            case "gateway" -> eurekaInstanceGw.getMetadataMap();
            case "cachingservice" -> cachingServiceEurekaInstanceConfigBean.getMetadataMap();
            case "apicatalog" -> {
                metadata = catalogEurekaInstanceConfigBean.getMetadataMap();
                if (isServerAttlsEnabled) {
                    var allowedOrigins = "https://" + hostname + ":" + port + "," + externalUrl;
                    metadata.put("apiml.corsEnabled", "true");
                    metadata.put("apiml.corsAllowedOrigins", allowedOrigins);
                }
                yield metadata;
            }
            default -> new HashMap<>();
        };

        String homePagePath = metadata.getOrDefault("apiml.homePagePath", "/");

        String scheme = "https";
        if (!https && !isServerAttlsEnabled) {
            scheme = "http";
        }

        boolean secure = https || isServerAttlsEnabled;
        return org.zowe.apiml.registry.model.ServiceInstance.builder()
            .instanceId(String.format("%s:%s:%d", hostname, serviceId, port))
            .appName(serviceId)
            .hostName(hostname)
            .homePageUrl(String.format("%s://%s:%d%s", scheme, hostname, port, homePagePath))
            .status(org.zowe.apiml.registry.model.InstanceStatus.UP)
            .ipAddr(ipAddress)
            .port(new org.zowe.apiml.registry.model.PortInfo(port, !secure))
            .securePort(new org.zowe.apiml.registry.model.PortInfo(port, secure))
            .vipAddress(serviceId)
            .secureVipAddress(serviceId)
            .dataCenterInfo(org.zowe.apiml.registry.model.DataCenterInfo.MY_OWN)
            .lease(leaseInfo)
            .lastUpdatedTimestamp(System.currentTimeMillis())
            .metadata(metadata)
            .build();
    }

    /**
     * The registry, injected.
     * <p>
     * Was a static lookup through {@code EurekaServerContextHolder} - a global singleton that had to be
     * initialised by hand from this class before anything could use it, and which made the wiring order
     * load-bearing and untestable.
     */
    private ServiceRegistry registry() {
        return applicationContext.getBean(ServiceRegistry.class);
    }

    void createLocalInstances() {
        instances.put(CoreService.GATEWAY.getServiceId(), getInstanceInfo(CoreService.GATEWAY.getServiceId()));
        instances.put(CoreService.DISCOVERY.getServiceId(), getInstanceInfo(CoreService.DISCOVERY.getServiceId()));
        instances.put(CoreService.CACHING.getServiceId(), getInstanceInfo(CoreService.CACHING.getServiceId()));
        instances.put(CoreService.API_CATALOG.getServiceId(), getInstanceInfo(CoreService.API_CATALOG.getServiceId()));

        var registry = registry();
        instances.values().forEach(instance -> registry.register(instance, RegistrationKind.STATIC));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStart() {
        createLocalInstances();

        log.info("Initialize timer for static services peer-replicated heartbeats");
        eventPublisher.publishEvent(new ApiCatalogServiceAvailableEvent(new Object()));

        // Keeps the peers' copies of the locally-registered core services alive. Their leases here are
        // permanent, but a peer holds them as ordinary renewable registrations and would evict them.
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                try {
                    applicationContext.getBean(PeerReplicator.class)
                        .replicateHeartbeat(instances.get(CoreService.GATEWAY.getServiceId()));
                } catch (RuntimeException e) {
                    log.debug("Peer replication is not available yet: {}", e.getMessage());
                }
            }

        }, registryConfig.getInstanceInfoReplicationIntervalSeconds() * 1000L,
            registryConfig.getInstanceInfoReplicationIntervalSeconds() * 1000L);

    }

    @Scheduled(initialDelay = 3000, fixedRate = 20_000) // TODO find better solution but DON'T JUST REMOVE!
    public void periodicJwtInit() {
        var jwtSec = applicationContext.getBean(JwtSecurity.class);
        var providers = applicationContext.getBean(Providers.class);
        if (providers.isZosfmUsed() && !jwtSec.getZosmfListener().isZosmfReady()) {
            // The registry client is disabled here - the registry is a bean in this JVM - so nothing
            // publishes RegistryCacheRefreshedEvent and this timer is what drives the check.
            jwtSec.getZosmfListener().onRegistryRefreshed();
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
    DiscoveryClient registryDiscoveryClient(ServiceRegistry serviceRegistry) {
        return new org.zowe.apiml.discovery.registry.RegistryDiscoveryClient(serviceRegistry);
    }

    /**
     * What this process counts as, when code needs to recognise its own registration.
     * <p>
     * The Gateway, because in the modulith one process is the Gateway, the Discovery Service, ZAAS, the
     * Caching Service and the API Catalog at once, and the Gateway is the registration the others are reached
     * through. The registry client's own {@code SelfRegistration} is not available here - the client is
     * disabled, see {@code eureka.client.enabled} in application.yml - so this supplies it.
     * <p>
     * Computed the same way as the registered instance rather than read from {@link #instances}, which is only
     * populated once the application is ready and would make this bean's value depend on startup order.
     */
    @Bean
    SelfRegistration selfRegistration() {
        var gateway = getInstanceInfo(CoreService.GATEWAY.getServiceId());
        return new SelfRegistration() {

            @Override
            public String instanceId() {
                return gateway.instanceId();
            }

            @Override
            public String serviceId() {
                return gateway.serviceId();
            }

        };
    }

    @Bean
    @Primary
    MessageService messageService() {
        MessageService messageService = YamlMessageServiceInstance.getInstance();
        messageService.loadMessages("/utility-log-messages.yml");
        messageService.loadMessages("/common-log-messages.yml");
        messageService.loadMessages("/security-common-log-messages.yml");

        messageService.loadMessages("/discovery-log-messages.yml");
        messageService.loadMessages("/gateway-log-messages.yml");
        messageService.loadMessages("/apicatalog-log-messages.yml");

        messageService.loadMessages("/zaas-log-messages.yml");

        messageService.loadMessages("/caching-log-messages.yml");
        return messageService;
    }

    @Bean
    public BasicInfoService basicInfoService(ServiceRegistry serviceRegistry, EurekaMetadataParser eurekaMetadataParser) {
        // The registry is a bean in this JVM, and ServiceRegistry is itself a RegistryView. This used to be an
        // anonymous subclass that passed null for the Eureka client and reimplemented getServicesInfo() over a
        // DiscoveryClient, because there was no way to hand it the registry it needed to read.
        return new BasicInfoService(serviceRegistry, eurekaMetadataParser);
    }

    @Bean
    @Primary
    TomcatReactiveWebServerFactory tomcatReactiveWebServerWithFiltersFactory(
        HttpHandler httpHandler,
        List<PreFluxFilter> preFluxFilters, ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,
        ObjectProvider<TomcatContextCustomizer> contextCustomizers,
        ObjectProvider<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers,
        List<ServletContextAware> servletContextAwareListeners) {

        var factory = new TomcatReactiveWebServerFactory() {
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
        factory.getTomcatConnectorCustomizers().addAll(connectorCustomizers.orderedStream().toList());
        factory.getTomcatContextCustomizers().addAll(contextCustomizers.orderedStream().toList());
        factory.getTomcatProtocolHandlerCustomizers().addAll(protocolHandlerCustomizers.orderedStream().toList());
        return factory;
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
