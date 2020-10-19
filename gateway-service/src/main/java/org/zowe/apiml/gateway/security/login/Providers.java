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
import org.springframework.security.authentication.AuthenticationServiceException;
import org.zowe.apiml.gateway.security.config.CompoundAuthProvider;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

@RequiredArgsConstructor
public class Providers {
    private final DiscoveryClient discoveryClient;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final CompoundAuthProvider compoundAuthProvider;

    /**
     * This method decides whether the Zosmf service is available.
     * @return Availability of the ZOSMF service in the system.
     * @throws AuthenticationServiceException if the z/OSMF service id is not configured
     */
    public boolean isZosmfAvailable() {
        return !this.discoveryClient.getInstances(authConfigurationProperties.validatedZosmfServiceId()).isEmpty();
    }

    /**
     * This method decides whether the Zosmf is used for authentication
     * @return Usage of the ZOSMF service in the system.
     */
    public boolean isZosfmUsed() {
        return compoundAuthProvider.getLoginAuthProviderName().equalsIgnoreCase(LoginProvider.ZOSMF.toString());
    }
}
