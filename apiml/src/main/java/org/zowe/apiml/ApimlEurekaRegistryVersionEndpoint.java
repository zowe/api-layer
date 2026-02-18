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

import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@RequiredArgsConstructor
@Endpoint(id = "eurekaversion")
@Slf4j
public class ApimlEurekaRegistryVersionEndpoint {

    private static final Pattern VERSION_PATTERN = Pattern.compile("UP_([0-9]+)_");

    private final PeerAwareInstanceRegistry peerAwareInstanceRegistry;

    @ReadOperation(produces = APPLICATION_JSON)
    public VersionDto status() {
        long version = -1;
        var hashCode = peerAwareInstanceRegistry.getApplications().getAppsHashCode();
        var matcher = VERSION_PATTERN.matcher(hashCode);
        if (matcher.find()) {
            version = Long.parseLong(matcher.group(1));
            log.debug("New Eureka registry version: {}", version);
        } else {
            log.debug("Unexpected Eureka registry hashCode: {}", hashCode);
        }
        return VersionDto.builder().version(version).build();
    }

    @Builder
    @Value
    static class VersionDto {

        private Long version;

    }


}
