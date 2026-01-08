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

import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.registry.ResponseCache;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Endpoint(id = "eurekaversion")
@Slf4j
public class ApimlEurekaRegistryVersionEndpoint {

    private final ResponseCache responseCache;

    public ApimlEurekaRegistryVersionEndpoint(EurekaServerContext eurekaServer) {
        var registry = eurekaServer.getRegistry();
        this.responseCache = registry.getResponseCache();
    }

    @ReadOperation(produces = APPLICATION_JSON)
    public VersionDto status() {
        return VersionDto.builder().version(responseCache.getVersionDelta().get()).build();
    }

    @Builder
    @Value
    static class VersionDto {

        private Long version;

    }


}
