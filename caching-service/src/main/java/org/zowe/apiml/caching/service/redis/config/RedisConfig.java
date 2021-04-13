/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.service.redis.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.caching.config.GeneralConfig;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(value = "caching.storage.redis")
@RequiredArgsConstructor
public class RedisConfig {
    private final GeneralConfig generalConfig;

    private String host;
    private Integer port = 6379;
    private Integer timeout = 60;
    private String username = "default";
    private String password = "";

    private Sentinel sentinel;
    private SslConfig ssl;

    public boolean usesSentinel() {
        return sentinel != null;
    }

    public boolean usesSsl() {
        return ssl != null && ssl.getEnabled();
    }

    @Data
    public static class Sentinel {
        private String master;
        private List<SentinelNode> nodes;

        @Data
        public static class SentinelNode {
            private String ip;
            private Integer port;
            private String password;
        }
    }

    @Data
    public static class SslConfig {
        private Boolean enabled = false;
        private String keyStore;
        private String keyStorePassword;
        private String trustStore;
        private String trustStorePassword;
    }
}
