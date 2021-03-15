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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.caching.config.GeneralConfig;

@Configuration
@Data
@ToString
@RequiredArgsConstructor
public class RedisConfig {
    private final GeneralConfig generalConfig;

    @Value("${caching.storage.redis.hostIP}")
    private String hostIP;

    @Value("${caching.storage.redis.port:6379}")
    private Integer port;

    @Value("${caching.storage.redis.timeout:60}")
    private Integer timeout;

    @Value("${caching.storage.redis.username:default}")
    private String username;

    @Value("${caching.storage.redis.password:}")
    private String password;
}
