/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/*
 * Copyright 2013-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zowe.apiml;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.converters.EurekaJacksonCodec;
import com.netflix.discovery.converters.wrappers.CodecWrapper;
import com.netflix.discovery.converters.wrappers.CodecWrappers;
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import com.netflix.discovery.shared.transport.jersey3.Jersey3TransportClientFactories;
import com.netflix.eureka.DefaultEurekaServerContext;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.*;
import com.netflix.eureka.transport.EurekaServerHttpClientFactory;
import com.netflix.eureka.transport.Jersey3EurekaServerHttpClientFactory;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.ext.Provider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;
import org.jvnet.hk2.spring.bridge.api.SpringBridge;
import org.jvnet.hk2.spring.bridge.api.SpringIntoHK2Bridge;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.netflix.eureka.EurekaConstants;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.server.*;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * @author Gunnar Hillert
 * @author Biju Kunjummen
 * @author Fahim Farook
 * @author Weix Sun
 * @author Robert Bleyl
 * @author Olga Maciaszek-Sharma
 */
@Configuration(proxyBeanMethods = false)
@Import(EurekaServerInitializerConfiguration.class)
@EnableConfigurationProperties({EurekaDashboardProperties.class, InstanceRegistryProperties.class,
    EurekaProperties.class})
@PropertySource("classpath:/eureka/server.properties")
public class EurekaConfiguration implements WebMvcConfigurer {

    private static final Log log = LogFactory.getLog(EurekaConfiguration.class);

    /**
     * List of packages containing Jersey resources required by the Eureka server.
     */
    private static final String[] EUREKA_PACKAGES = new String[]{"com.netflix.discovery", "com.netflix.eureka"};

    /**
     * Static content pattern for dashboard elements (images, css, etc...).
     */
    private static final String STATIC_CONTENT_PATTERN = "/(fonts|images|css|js)/.*";

    @Autowired
    private ApplicationInfoManager applicationInfoManager;

    @Autowired
    private EurekaServerConfig eurekaServerConfig;

    @Autowired
    private EurekaClientConfig eurekaClientConfig;

    @Autowired
    private EurekaClient eurekaClient;

    @Autowired
    private InstanceRegistryProperties instanceRegistryProperties;

    /**
     * A {@link CloudJacksonJson} instance.
     */
    public static final CloudJacksonJson JACKSON_JSON = new CloudJacksonJson();

    @Bean
    HasFeatures eurekaServerFeature() {
        return HasFeatures.namedFeature("Eureka Server", EurekaServerAutoConfiguration.class);
    }

    static {
        CodecWrappers.registerWrapper(JACKSON_JSON);
        EurekaJacksonCodec.setInstance(JACKSON_JSON.getCodec());
    }

    @Bean
    ServerCodecs serverCodecs() {
        return new CloudServerCodecs(this.eurekaServerConfig);
    }


    @Bean
    @ConditionalOnMissingBean
    ReplicationClientAdditionalFilters replicationClientAdditionalFilters() {
        return new ReplicationClientAdditionalFilters(Collections.emptySet());
    }

    @Bean
    @ConditionalOnMissingBean(TransportClientFactories.class)
    Jersey3TransportClientFactories jersey3TransportClientFactories() {
        return Jersey3TransportClientFactories.getInstance();
    }

    @Bean
    @ConditionalOnMissingBean(EurekaServerHttpClientFactory.class)
    Jersey3EurekaServerHttpClientFactory jersey3EurekaServerHttpClientFactory() {
        return new Jersey3EurekaServerHttpClientFactory();
    }

    @Bean
    ApplicationsResource applicationsResource() {
        return new ApplicationsResource();
    }

    @Bean
    VIPResource vipResource() {
        return new VIPResource();
    }

    @Bean
    ServerInfoResource serverInfoResource() {
        return new ServerInfoResource();
    }

    @Bean
    SecureVIPResource secureVIPResource() {
        return new SecureVIPResource();
    }

    @Bean
    InstancesResource instancesResource() {
        return new InstancesResource();
    }

    @Bean
    ASGResource asgResource() {
        return new ASGResource();
    }

    @Bean
    PeerReplicationResource peerReplicationResource() {
        return new PeerReplicationResource();
    }

    @Bean
    PeerAwareInstanceRegistry peerAwareInstanceRegistry(ServerCodecs serverCodecs,
                                                        EurekaServerHttpClientFactory eurekaServerHttpClientFactory,
                                                        EurekaInstanceConfigBean eurekaInstanceConfigBean) {
        if (eurekaInstanceConfigBean.isAsyncClientInitialization()) {
            if (log.isDebugEnabled()) {
                log.debug("Initializing client asynchronously...");
            }

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                this.eurekaClient.getApplications();
                if (log.isDebugEnabled()) {
                    log.debug("Asynchronous client initialization done.");
                }
            });
        } else {
            this.eurekaClient.getApplications(); // force initialization
        }

        return new InstanceRegistry(this.eurekaServerConfig, this.eurekaClientConfig, serverCodecs, this.eurekaClient,
            eurekaServerHttpClientFactory,
            this.instanceRegistryProperties.getExpectedNumberOfClientsSendingRenews(),
            this.instanceRegistryProperties.getDefaultOpenForTrafficCount());
    }

    @Bean
    @ConditionalOnMissingBean
    EurekaServerContext eurekaServerContext(ServerCodecs serverCodecs, PeerAwareInstanceRegistry registry,
                                            PeerEurekaNodes peerEurekaNodes) {
        return new DefaultEurekaServerContext(this.eurekaServerConfig, serverCodecs, registry, peerEurekaNodes,
            this.applicationInfoManager);
    }

    @Bean
    EurekaServerBootstrap eurekaServerBootstrap(PeerAwareInstanceRegistry registry,
                                                EurekaServerContext serverContext) {
        return new EurekaServerBootstrap(this.applicationInfoManager, this.eurekaClientConfig, this.eurekaServerConfig,
            registry, serverContext);
    }

    /**
     * Register the Jersey filter.
     *
     * @param eurekaJerseyApp an {@link Application} for the filter to be registered
     * @return a jersey {@link FilterRegistrationBean}
     */
    @Bean
    FilterRegistrationBean<?> jerseyFilterRegistration(ResourceConfig eurekaJerseyApp) {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        ServletContainer servletContainer = new ServletContainer(eurekaJerseyApp);
        bean.setFilter(servletContainer);
        bean.setOrder(Ordered.LOWEST_PRECEDENCE);
        bean.setUrlPatterns(Collections.singletonList(EurekaConstants.DEFAULT_PREFIX + "/*"));

        return bean;
    }

    @Bean
    FilterRegistrationBean<?> eurekaVersionFilterRegistration(ServerProperties serverProperties,
                                                              Environment env) {
        final String contextPath = serverProperties.getServlet().getContextPath();
        String regex = EurekaConstants.DEFAULT_PREFIX + STATIC_CONTENT_PATTERN;
        if (StringUtils.hasText(contextPath)) {
            regex = contextPath + regex;
        }
        String debugResponseHeader = env.getProperty("eureka.server.version.filter.debug.response-header");
        boolean addDebugResponseHeader = StringUtils.hasText(debugResponseHeader);
        Pattern staticPattern = Pattern.compile(regex);
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                HttpServletRequest req = request;
                String requestURI = request.getRequestURI();
                if (!requestURI.startsWith(EurekaConstants.DEFAULT_PREFIX + "/v2")
                    // don't forward static requests (images, js, etc...) to /v2
                    && !staticPattern.matcher(requestURI).matches()) {

                    String prefix = EurekaConstants.DEFAULT_PREFIX;
                    if (StringUtils.hasText(contextPath)) {
                        prefix = contextPath + prefix;
                    }
                    String updatedPath = EurekaConstants.DEFAULT_PREFIX + "/v2" + requestURI.substring(prefix.length());
                    if (StringUtils.hasText(contextPath)) {
                        updatedPath = contextPath + updatedPath;
                    }
                    final String computedPath = updatedPath;
                    // only used if a special debug property is set, so in prod this is
                    // always skipped.
                    if (addDebugResponseHeader) {
                        response.addHeader(debugResponseHeader, computedPath);
                    }
                    HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
                        @Override
                        public String getRequestURI() {
                            return computedPath;
                        }

                        @Override
                        public String getServletPath() {
                            return computedPath;
                        }
                    };
                    req = wrapper;
                }
                filterChain.doFilter(req, response);
            }
        });
        bean.setOrder(0);
        bean.setUrlPatterns(Collections.singletonList(EurekaConstants.DEFAULT_PREFIX + "/*"));

        return bean;
    }

    /**
     * Construct a Jersey {@link jakarta.ws.rs.core.Application} with all the resources
     * required by the Eureka server.
     *
     * @param environment    an {@link Environment} instance to retrieve classpath resources
     * @param resourceLoader a {@link ResourceLoader} instance to get classloader from
     * @return created {@link Application} object
     */
    @Bean
    ResourceConfig jerseyApplication(Environment environment, ResourceLoader resourceLoader,
                                     BeanFactory beanFactory) {

        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false,
            environment);

        // Filter to include only classes that have a particular annotation.
        //
        provider.addIncludeFilter(new AnnotationTypeFilter(Path.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(Provider.class));

        // Find classes in Eureka packages (or subpackages)
        //
        Set<Class<?>> classes = new HashSet<>();
        for (String basePackage : EUREKA_PACKAGES) {
            Set<BeanDefinition> beans = provider.findCandidateComponents(basePackage);
            for (BeanDefinition bd : beans) {
                Class<?> cls = ClassUtils.resolveClassName(bd.getBeanClassName(), resourceLoader.getClassLoader());
                classes.add(cls);
            }
        }

        // https://javaee.github.io/hk2/spring-bridge

        // Construct the Jersey ResourceConfig
        ResourceConfig rc = new ResourceConfig(classes).property(
            // Skip static content used by the webapp
            ServletProperties.FILTER_STATIC_CONTENT_REGEX, EurekaConstants.DEFAULT_PREFIX + STATIC_CONTENT_PATTERN);

        rc.register(new ContainerLifecycleListener() {
            @Override
            public void onStartup(Container container) {
                ServiceLocator serviceLocator = container.getApplicationHandler()
                    .getInjectionManager()
                    .getInstance(ServiceLocator.class);
                SpringBridge.getSpringBridge().initializeSpringBridge(serviceLocator);
                serviceLocator.getService(SpringIntoHK2Bridge.class).bridgeSpringBeanFactory(beanFactory);
            }

            @Override
            public void onReload(Container container) {

            }

            @Override
            public void onShutdown(Container container) {

            }
        });

        return rc;
    }

    @Bean
    @ConditionalOnBean(name = "httpTraceFilter")
    FilterRegistrationBean<?> traceFilterRegistration(@Qualifier("httpTraceFilter") Filter filter) {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter(filter);
        bean.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
        return bean;
    }

    @Configuration(proxyBeanMethods = false)
    protected static class EurekaServerConfigBeanConfiguration {

        @Bean
        @ConditionalOnMissingBean
        EurekaServerConfig eurekaServerConfig(EurekaClientConfig clientConfig) {
            EurekaServerConfigBean server = new EurekaServerConfigBean();
            if (clientConfig.shouldRegisterWithEureka()) {
                // Set a sensible default if we are supposed to replicate
                server.setRegistrySyncRetries(5);
            }
            return server;
        }

    }

    class CloudServerCodecs extends DefaultServerCodecs {

        CloudServerCodecs(EurekaServerConfig serverConfig) {
            super(getFullJson(serverConfig), CodecWrappers.getCodec(CodecWrappers.JacksonJsonMini.class),
                getFullXml(serverConfig), CodecWrappers.getCodec(CodecWrappers.JacksonXmlMini.class));
        }

        private static CodecWrapper getFullJson(EurekaServerConfig serverConfig) {
            CodecWrapper codec = CodecWrappers.getCodec(serverConfig.getJsonCodecName());
            return codec == null ? CodecWrappers.getCodec(JACKSON_JSON.codecName()) : codec;
        }

        private static CodecWrapper getFullXml(EurekaServerConfig serverConfig) {
            CodecWrapper codec = CodecWrappers.getCodec(serverConfig.getXmlCodecName());
            return codec == null ? CodecWrappers.getCodec(CodecWrappers.XStreamXml.class) : codec;
        }

    }
}
