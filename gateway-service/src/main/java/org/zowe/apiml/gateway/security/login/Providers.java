/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

@RequiredArgsConstructor
public class Providers {
    private final DiscoveryClient discoveryClient;
    private final AuthConfigurationProperties authConfigurationProperties;

    /**
     * This method decides whether the Zosmf service is available.
     * @return Availability of the ZOSMF service in the system.
     */
    public boolean isZosmfAvailable() {
        return !this.discoveryClient.getInstances(authConfigurationProperties.validatedZosmfServiceId()).isEmpty();
    }

    /**
     * This method decides whether the Zosmf is used for authentication
     * @return Usage of the ZOSMF service in the system.
     */
    public boolean isZosfmUsed() {
        return authConfigurationProperties.getProvider().equalsIgnoreCase(LoginProvider.ZOSMF.toString());
    }
}
