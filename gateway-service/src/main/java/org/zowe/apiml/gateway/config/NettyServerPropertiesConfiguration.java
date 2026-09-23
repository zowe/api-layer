/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.reactor.netty.autoconfigure.NettyServerProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the Netty server properties that Spring Cloud Gateway requires, without letting Netty
 * become the web server.
 * <p>
 * APIML services are reactive ({@code spring.main.web-application-type=reactive}) but are expected to
 * run on Tomcat. Several APIML components depend on that, most visibly
 * {@code org.zowe.apiml.gateway.websocket.ApimlRequestUpgradeStrategy}, which extends Spring's
 * servlet-based {@code StandardWebSocketUpgradeStrategy} and reads the native request as a
 * {@code jakarta.servlet.http.HttpServletRequest}. The Tomcat customizers in
 * {@code apiml-tomcat-common} ({@code ApimlTomcatCustomizer}, {@code TomcatKeyringFix},
 * {@code TomcatAcceptFixConfig}) are servlet- and connector-based as well.
 * <p>
 * Spring Boot selects the reactive server from the classpath, and both
 * {@code TomcatReactiveWebServerAutoConfiguration} and {@code NettyReactiveWebServerAutoConfiguration}
 * are guarded only by {@code @ConditionalOnMissingBean(ReactiveWebServerFactory.class)}.
 * Auto-configurations are ordered by class name, so {@code org.springframework.boot.reactor.netty...} is
 * processed before {@code org.springframework.boot.tomcat...} and Netty wins whenever reactor-netty is
 * present. reactor-netty cannot simply be excluded, because Spring Cloud Gateway's
 * {@code NettyConfiguration} is active regardless of the server: it defines the {@code HttpClientProperties}
 * bean that APIML injects (for example for the WebSocket frame limit) and that configuration cannot be
 * built without {@code NettyServerProperties}.
 * <p>
 * The resolution is to exclude only {@code NettyReactiveWebServerAutoConfiguration} (see
 * {@code spring.autoconfigure.exclude} in {@code application.yml}) so Spring Boot's own Tomcat reactive
 * auto-configuration runs normally -- it applies the {@code TomcatConnectorCustomizer},
 * {@code TomcatContextCustomizer} and {@code TomcatProtocolHandlerCustomizer} beans inside its factory
 * method, so declaring the factory bean here instead would silently drop them -- and to re-declare
 * {@code NettyServerProperties}, which the excluded auto-configuration would otherwise have registered.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(NettyServerProperties.class)
@EnableConfigurationProperties(NettyServerProperties.class)
public class NettyServerPropertiesConfiguration {
}
