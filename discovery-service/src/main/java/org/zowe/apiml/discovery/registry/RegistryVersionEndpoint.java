/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import org.zowe.apiml.registry.ServiceRegistry;

import java.util.regex.Pattern;

/**
 * The {@code /application/eurekaversion} actuator endpoint, served from the local registry.
 * <p>
 * Despite the name, the number reported is <b>the count of UP instances</b>, not a version. It is derived by
 * matching {@code UP_(\d+)_} against the registry hash code, which is what the Eureka-based endpoints did and
 * still do.
 * <p>
 * That definition is preserved deliberately, even though this registry has a genuine monotonic version available
 * in {@code Applications.version()}. The integration startup check compares this value <em>between</em> APIML
 * instances to decide whether they have converged, and the clients - the Caching Service, the discoverable client
 * and anything using {@code EurekaRegistryVersionEndpoint} - still compute it from a Eureka client's hash code.
 * Reporting a monotonic counter here while they report an instance count would make those comparisons meaningless.
 * Switching to the real version belongs with Phase 5, when the clients move across, and needs doing on both sides
 * at once.
 */
@Component
@Endpoint(id = "eurekaversion")
@RequiredArgsConstructor
@Slf4j
public class RegistryVersionEndpoint {

    private static final Pattern VERSION_PATTERN = Pattern.compile("UP_(\\d+)_");

    private final ServiceRegistry registry;

    @ReadOperation
    public VersionDto status() {
        String hashCode = registry.applications().appsHashCode();
        var matcher = VERSION_PATTERN.matcher(hashCode == null ? "" : hashCode);
        if (matcher.find()) {
            return VersionDto.builder().version(Long.parseLong(matcher.group(1))).build();
        }
        // No UP instances yet. -1 is what the Eureka-based endpoint reported before its first registry update,
        // and the startup checker treats it as "not ready".
        log.debug("No UP instances in registry hash code '{}'", hashCode);
        return VersionDto.builder().version(-1L).build();
    }

    @Builder
    @Value
    public static class VersionDto {

        Long version;

    }

}
