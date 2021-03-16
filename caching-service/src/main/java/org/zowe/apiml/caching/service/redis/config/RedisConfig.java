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
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.caching.config.GeneralConfig;

import java.util.List;

@Configuration
@ConfigurationProperties("caching.storage.redis")
@Data
@ToString
@RequiredArgsConstructor
public class RedisConfig {
    private final GeneralConfig generalConfig;

    private String masterIP;

    private Integer masterPort = 6379;

    private Integer timeout = 60;

    private String username = "default";

    private String password = "";

    private Sentinel sentinel;

    public boolean usesSentinel() {
        return sentinel != null;
    }

    @Data
    @ToString
    @ConfigurationProperties("caching.storage.redis.sentinel")
    public static class Sentinel {
        private String master;
        private List<SentinelNode> nodes;

        @Data
        @ToString
        public static class SentinelNode {
            private String ip;
            private Integer port;
        }
    }
}
