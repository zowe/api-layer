/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.eureka.web;

import com.google.common.base.Objects;
import com.netflix.appinfo.InstanceInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zowe.apiml.exception.MetadataValidationException;

/**
 * Applies the domain allow list to a Netflix {@code InstanceInfo}.
 * <p>
 * A bridge, and a temporary one. {@link MetadataFilterService} no longer knows about Netflix types, which is what
 * lets the Discovery Service compile without Eureka on its classpath at all. The API Catalog is still a Eureka
 * client and still holds {@code InstanceInfo} objects, so it needs this until Phase 5 moves it onto the
 * replacement client - at which point this class is deleted.
 * <p>
 * Kept in a separate class rather than as an overload on {@link MetadataFilterService} on purpose: an overload
 * would force every caller of that service to have {@code InstanceInfo} resolvable, which defeats the point.
 */
@Service
@RequiredArgsConstructor
public class EurekaMetadataFilterAdapter {

    private final MetadataFilterService metadataFilterService;

    public InstanceInfo verifyAllowedDomains(InstanceInfo info) throws MetadataValidationException {
        var candidate = new MetadataFilterService.Candidate(
            info.getInstanceId(),
            info.getIPAddr(),
            info.getHostName(),
            info.getHomePageUrl(),
            info.getHealthCheckUrl(),
            info.getStatusPageUrl(),
            info.getSecureHealthCheckUrl(),
            info.getMetadata()
        );
        var allowedIpAddr = metadataFilterService.verifyAllowedDomains(candidate);
        if (!Objects.equal(allowedIpAddr, info.getIPAddr())) {
            // Looks like it builds a new instance, but Builder(info) mutates the same InstanceInfo.
            info = new InstanceInfo.Builder(info).setIPAddr(allowedIpAddr).build();
        }
        return info;
    }

}
