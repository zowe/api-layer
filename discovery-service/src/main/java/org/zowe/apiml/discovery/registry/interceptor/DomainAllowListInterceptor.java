/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry.interceptor;

import lombok.RequiredArgsConstructor;
import org.zowe.apiml.product.eureka.web.MetadataFilterService;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.registry.spi.RegistrationInterceptor;

/**
 * Applies the configured domain allow list to a registration.
 * <p>
 * Delegates to the shared {@link MetadataFilterService}, so the standalone Discovery Service and the modulith
 * enforce exactly the same policy. A disallowed IP address is corrected to the address resolved from the hostname
 * rather than rejected - long-standing behaviour, because a service behind NAT reports an address the registry
 * cannot verify. Anything else disallowed throws, unless the service is configured to warn only.
 */
@RequiredArgsConstructor
public class DomainAllowListInterceptor implements RegistrationInterceptor {

    private final MetadataFilterService metadataFilterService;

    @Override
    public ServiceInstance intercept(ServiceInstance instance) {
        var candidate = new MetadataFilterService.Candidate(
            instance.instanceId(),
            instance.ipAddr(),
            instance.hostName(),
            instance.homePageUrl(),
            instance.healthCheckUrl(),
            instance.statusPageUrl(),
            instance.secureHealthCheckUrl(),
            instance.metadata()
        );

        String allowedIpAddr = metadataFilterService.verifyAllowedDomains(candidate);
        if (allowedIpAddr != null && !allowedIpAddr.equals(instance.ipAddr())) {
            return instance.toBuilder().ipAddr(allowedIpAddr).build();
        }
        return instance;
    }

    @Override
    public int order() {
        return 30;
    }

}
