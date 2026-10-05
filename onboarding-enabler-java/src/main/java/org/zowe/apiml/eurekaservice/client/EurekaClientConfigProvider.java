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
import org.zowe.apiml.eurekaservice.client.config.EurekaClientConfiguration;

import jakarta.inject.Provider;

/**
 * Provides a client configuration based on the provided ApiMl service configuration.
 * <p>
 * The type it provides changed with the registry-client migration: it used to be Netflix's
 * {@code EurekaClientConfig}, it now is the enabler's own {@link EurekaClientConfiguration}. The interface and its
 * role as the extension point are unchanged.
 */
public interface EurekaClientConfigProvider extends Provider<EurekaClientConfiguration> {

    /**
     * @param config Configuration for the service
     * @return Valid client configuration for the Discovery service
     */
    EurekaClientConfiguration config(final ApiMediationServiceConfig config);
}
