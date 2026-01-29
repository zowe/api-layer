/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.api;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;

import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;

@Component
@RequiredArgsConstructor
@Endpoint(id = "eurekaversion")
@Slf4j
public class ClientEurekaRegistryVersionEndpoint {

    private static final Pattern VERSION_PATTERN = Pattern.compile("_([0-9]+)_");

    private final ApiMediationClient apiMediationClient;

    @ReadOperation(produces = APPLICATION_JSON)
    public VersionDto status() {
        long version = -1;
        var eurekaClient = apiMediationClient.getEurekaClient();
        if (eurekaClient != null) {
            var hashCode = eurekaClient.getApplications().getAppsHashCode();
            var matcher = VERSION_PATTERN.matcher(hashCode);
            if (matcher.matches()) {
                version = Long.parseLong(matcher.group(1));
            } else {
                log.debug("Unexpected Eureka registry hashCode: {}", hashCode);
            }
        }
        return VersionDto.builder().version(version).build();
    }

    @Builder
    @Value
    static class VersionDto {

        private Long version;

    }

}
