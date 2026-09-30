/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.zowe.apiml.registry.client.RegistryClient;

import java.util.regex.Pattern;

/**
 * The {@code /application/eurekaversion} actuator endpoint, served from this service's registry view.
 * <p>
 * Every service used to have one of these: the endpoint lived in {@code apiml-common}, which they all depend on,
 * and read the version out of its own Eureka client's applications hash code. When it moved to the Discovery
 * Service, the Gateway, ZAAS and the API Catalog were left without it.
 * <p>
 * Nothing failed to compile and nothing failed a unit test, but the integration startup check reads this endpoint
 * on <em>every</em> instance to decide whether they have converged, so the instances that no longer answered it
 * were reported as never coming up:
 * <pre>
 *   Eurekaversion endpoint is not accessible on localhost:gateway:10010
 *   Eurekaversion endpoint is not accessible on localhost:zaas:10023
 *   Eurekaversion endpoint is not accessible on localhost:apicatalog:10014
 * </pre>
 * The startup check gates every integration test job, so the whole suite failed on it.
 * <p>
 * The number reported is <b>the count of UP instances</b>, not a version - it is derived by matching
 * {@code UP_(\d+)_} against the registry hash code, exactly as the Eureka-based endpoints did and as the
 * Discovery Service's own replacement still does. The check compares the value <em>between</em> instances, so
 * reporting anything else here would make the comparison meaningless. See
 * {@code discovery-service}'s {@code RegistryVersionEndpoint}, which documents the same choice.
 * <p>
 * Declared as a bean by {@link RegistryClientAutoConfiguration} rather than as a component, and only when
 * actuator is on the classpath. A service that serves this endpoint from its own registry - the Discovery
 * Service, the Caching Service, the discoverable client - must not also depend on this module: two endpoints
 * with the same id stop the application from starting.
 */
@Endpoint(id = "eurekaversion")
@RequiredArgsConstructor
@Slf4j
public class RegistryClientVersionEndpoint {

    private static final Pattern VERSION_PATTERN = Pattern.compile("UP_(\\d+)_");

    private final RegistryClient client;

    @ReadOperation
    public VersionDto status() {
        var applications = client.cache().applications();
        String hashCode = applications == null ? null : applications.appsHashCode();
        var matcher = VERSION_PATTERN.matcher(hashCode == null ? "" : hashCode);
        if (matcher.find()) {
            return VersionDto.builder().version(Long.parseLong(matcher.group(1))).build();
        }
        // No UP instances in this service's view yet. -1 is what the Eureka-based endpoint reported before its
        // first registry update, and the startup checker treats it as "not ready".
        log.debug("No UP instances in registry hash code '{}'", hashCode);
        return VersionDto.builder().version(-1L).build();
    }

    @Builder
    @Value
    public static class VersionDto {

        Long version;

    }

}
