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
import com.netflix.discovery.EurekaClient;

/**
 *  Defines {@link ApiMediationClient} methods for registering and unregistering REST service with API Mediation Layer
 *  Discovery service. Registration method creates an instance of {@link com.netflix.discovery.EurekaClient} which is
 *  stored in a member variable for later use. The client instance is internally used during unregistering.
 *  Getter method is provided for accessing the instance.
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
     * The Eureka client used to register the service and communicate with Eureka server.
     *
     * @return
     */
    EurekaClient getEurekaClient();
}
