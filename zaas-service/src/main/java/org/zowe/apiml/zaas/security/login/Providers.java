/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.login;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class Providers {
    private final DiscoveryClient discoveryClient;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final CompoundAuthProvider compoundAuthProvider;
    private final ZosmfService zosmfService;

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    /**
     * This method decides whether the Zosmf service is available.
     *
     * @return Availability of the ZOSMF service in the system.
     * @throws AuthenticationServiceException if the z/OSMF service id is not configured
     */
    public boolean isZosmfAvailable() {
        String  zosmfServiceId = authConfigurationProperties.validatedZosmfServiceId();

        try {
            this.discoveryClient.probe();
            List<String> ids = this.discoveryClient.getServices();
            if (!ids.isEmpty()) {
                boolean isZosmfRegisteredAndPropagated = !this.discoveryClient.getInstances(zosmfServiceId).isEmpty();
                if (!isZosmfRegisteredAndPropagated) {
                    apimlLog.log("org.zowe.apiml.security.auth.zosmf.serviceId", zosmfServiceId);
                }
                log.debug("z/OSMF registered with the Discovery Service and propagated to ZAAS: {}", isZosmfRegisteredAndPropagated);
                return isZosmfRegisteredAndPropagated;
            } else {
                throw new ServiceNotAccessibleException("No registered services");
            }
        } catch (RuntimeException e) {
            log.debug("Discovery Service is not available yet: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Provide configured z/OSMF service ID from the ZAAS configuration.
     *
     * @return service ID of z/OSMF instance
     */
    public String getZosmfServiceId() {
        return authConfigurationProperties.validatedZosmfServiceId();
    }

    /**
     * Verify that the zOSMF is registered in the Discovery service and that we can actually reach it.
     *
     * @return true if the service is registered and properly responds.
     */
    public boolean isZosmfAvailableAndOnline() {
        try {
            boolean isAvailable = isZosmfAvailable();
            boolean isAccessible = zosmfService.isAccessible();

            log.debug("z/OSMF is registered and propagated to the DS: {} and is accessible based on the information: {}", isAvailable, isAccessible);

            return isAvailable && isAccessible;
        } catch (ServiceNotAccessibleException e) {
            log.debug("z/OSMF is not registered to the ZAAS yet: {}", e.getMessage());

            return false;
        }
    }

    /**
     * This method decides whether the Zosmf is used for authentication
     *
     * @return Usage of the ZOSMF service in the system.
     */
    public boolean isZosfmUsed() {
        return compoundAuthProvider.getLoginAuthProviderName().equalsIgnoreCase(LoginProvider.ZOSMF.toString());
    }

    /**
     * This method decides whether used zOSMF instance supports JWT tokens.
     *
     * @return True is the instance support JWT
     */
    public boolean zosmfSupportsJwt() {
        switch (authConfigurationProperties.getZosmf().getJwtAutoconfiguration().toUpperCase()) {
            case "JWT":
                return true;
            case "LTPA":
                return false;
            default:
                throw new IllegalArgumentException(
                    String.format("Unknown provider is given, Please verify if the provider is supported" ));
        }
    }

    /**
     * This method is used to access configuration provided by the user to determine if zOSMF supports LTPA token
     * instead of JWT.
     *
     * @return true if configuration was set to indicate zOSMF supports LTPA.
     */
    public boolean isZosmfConfigurationSetToLtpa() {
        return authConfigurationProperties.getZosmf().getJwtAutoconfiguration().equalsIgnoreCase("LTPA");
    }
}
