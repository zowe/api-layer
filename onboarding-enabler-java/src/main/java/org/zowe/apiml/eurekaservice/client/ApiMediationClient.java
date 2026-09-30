/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.eurekaservice.client;

import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.registry.client.RegistryClient;


/**
 * Defines {@link ApiMediationClient} methods for registering and unregistering REST service with API Mediation Layer
 * Discovery service. Registration method creates an instance of {@link RegistryClient} which is stored in a member
 * variable for later use. The client instance is internally used during unregistering. Getter method is provided for
 * accessing the instance. isRegistered method is provided to indicate if the client has successfully registered with
 * the Discovery Service.
 */
public interface ApiMediationClient {
    /**
     * Register the service described by the ApiMediationServiceConfig configuration object.
     *
     * @param config
     * @throws ServiceDefinitionException - checked exception encapsulating the real reason why registration has failed.
     */
    void register(ApiMediationServiceConfig config) throws ServiceDefinitionException;

    /**
     * Entry point for unregistering and clean up.
     */
    void unregister();

    /**
     * The registry client used to register the service and communicate with the Discovery Service.
     * <p>
     * Replaces {@code getEurekaClient()}: the enabler no longer runs a Netflix client, it runs the API ML registry
     * client against the same wire protocol. The returned client is passive until {@link #register} has been called;
     * {@code null} afterwards only when {@link #unregister} has torn it down.
     *
     * @return the registry client, or null before the first registration and after unregistering
     */
    RegistryClient getRegistryClient();

    /**
     * @return boolean indicating if the client is registered with the Discovery Service.
     */
    default boolean isRegistered() {
        RegistryClient registryClient = getRegistryClient();
        return registryClient != null && registryClient.registered();
    }
}
