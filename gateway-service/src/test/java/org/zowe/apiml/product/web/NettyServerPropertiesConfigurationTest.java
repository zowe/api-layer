/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.reactor.netty.autoconfigure.NettyServerProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * APIML runs reactive services on Tomcat, so {@code NettyReactiveWebServerAutoConfiguration} is
 * excluded and Netty must not become the server. Spring Cloud Gateway's {@code NettyConfiguration}
 * is still active though, and it needs the {@code NettyServerProperties} bean the excluded
 * auto-configuration would have registered. This asserts the replacement configuration keeps that
 * bean available.
 */
class NettyServerPropertiesConfigurationTest {

    @Configuration(proxyBeanMethods = false)
    @Import(NettyServerPropertiesConfiguration.class)
    static class TestConfiguration {
    }

    @Test
    void givenReactorNettyOnTheClasspath_whenConfigurationIsApplied_thenNettyServerPropertiesIsAvailable() {
        new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(NettyServerProperties.class);
            });
    }

}
