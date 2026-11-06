/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.websocket;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.zowe.apiml.gateway.config.WebSocketConfig;
import org.zowe.apiml.product.web.HttpConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebSocketPropertiesTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(WebSocketConfig.class)
        .withBean(HttpClientProperties.class, HttpClientProperties::new)
        .withBean(HttpConfig.class, () -> mock(HttpConfig.class))
        .withBean(WebSocketService.class, () -> mock(WebSocketService.class))
        .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @ParameterizedTest
    @ValueSource(strings = {"server.webSocket", "server.web-socket"})
    void bindsLauncherAndCanonicalPropertyNamesAndEnablesPartialForwarding(String prefix) {
        context.withPropertyValues(prefix + ".partial.enabled=true", prefix + ".partial.maxBinaryMessageSize=16777216",
            prefix + ".asyncWriteTimeout=1234").run(application -> {
                assertThat(application).hasNotFailed();
                var properties = application.getBean(WebSocketProperties.class);
                assertThat(properties.getPartial().isEnabled()).isTrue();
                assertThat(properties.getPartial().getMaxBinaryMessageSize()).isEqualTo(16777216);
                assertThat(properties.getAsyncWriteTimeout()).isEqualTo(1234);
                assertThat(application).hasSingleBean(WebSocketRelayResources.class)
                    .hasSingleBean(WsWebSocketContainer.class)
                    .hasSingleBean(ApimlWebSocketRoutingFilter.class);
            });
    }

    @ParameterizedTest
    @ValueSource(strings = {"server.webSocket", "server.web-socket"})
    void disablesPartialForwardingForBothPropertyNames(String prefix) {
        context.withPropertyValues(prefix + ".partial.enabled=false").run(application -> {
            assertThat(application).hasNotFailed();
            assertThat(application).doesNotHaveBean(WebSocketRelayResources.class)
                .doesNotHaveBean(WsWebSocketContainer.class)
                .hasSingleBean(ApimlWebSocketRoutingFilter.class);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"partial.pendingBytes=1", "partial.pendingParts=1", "partial.globalPendingBytes=1",
        "partial.binaryBufferSize=0", "partial.writerThreads=0", "partial.maxConnections=0", "partial.maxBinaryMessageSize=-1",
        "partial.messageTimeout=0", "asyncWriteTimeout=0", "connectTimeout=0", "maxIdleTimeout=-1"})
    void rejectsInvalidBudgetsAndTimeoutsAtStartup(String setting) {
        context.withPropertyValues("server.webSocket." + setting).run(application -> assertThat(application).hasFailed());
    }

    @Test
    void defaultsToPartialForwardingAndLegacyMessageLimits() {
        context.run(application -> {
            var properties = application.getBean(WebSocketProperties.class);
            assertThat(properties.getPartial().isEnabled()).isTrue();
            assertThat(properties.getPartial().getMaxBinaryMessageSize()).isZero();
            assertThat(properties.getPartial().getMaxTextMessageSize()).isZero();
            assertThat(application).hasSingleBean(WebSocketRelayResources.class)
                .hasSingleBean(WsWebSocketContainer.class)
                .hasSingleBean(ApimlWebSocketRoutingFilter.class);
        });
    }
}
