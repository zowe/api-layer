/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.register;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.runtime.event.annotation.EventListener;
import org.zowe.apiml.config.DiscoveryClientConfig;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ApiMlRegistrar {
    @Inject
    DiscoveryClientConfig config;

    @Value("${apiml:enabled:true}")
    boolean apimlEnabled;

    @Inject
    ApiMediationClient apiMlClient;

    @InjectApimlLogger
    private ApimlLogger logger = ApimlLogger.empty();

    @EventListener
    @Retryable
    void onStartupEvent(StartupEvent event) throws ServiceDefinitionException {
        apiMlClient.register(config);
    }

    @EventListener
    void onShutDownEvent(ShutdownEvent event) {
        apiMlClient.unregister();
    }
}
