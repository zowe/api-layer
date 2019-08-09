/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.instance.lookup;

import com.ca.mfaas.product.instance.InstanceNotFoundException;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public class InstanceLookupExecutor {

    private static final int INITIAL_DELAY = 100;
    private static final int PERIOD = 5000;

    private final EurekaClient eurekaClient;
    private final ScheduledExecutorService executorService =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "InstanceLookupExecutor-Thread");
            thread.setDaemon(true);
            return thread;
        }
    );

    public InstanceLookupExecutor(EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
    }

    private InstanceInfo findEurekaInstance(String serviceId) {
        Application application = eurekaClient.getApplication(serviceId);
        if (application == null) {
            throw new InstanceNotFoundException("No " + serviceId + " Application is registered in Discovery Client");
        }

        List<InstanceInfo> appInstances = application.getInstances();
        if (appInstances.isEmpty()) {
            throw new InstanceNotFoundException("No " + serviceId + " Instances registered within Application in Discovery Client");
        }

        return appInstances.get(0);
    }


    public void run(String serviceId,
                    Consumer<InstanceInfo> action,
                    BiConsumer<Exception, Boolean> handleFailureConsumer) {
        log.debug("Started instance finder");

        executorService.scheduleAtFixedRate(
            () -> {
                try {
                    InstanceInfo instanceInfo = findEurekaInstance(serviceId);
                    log.debug("App found {}", instanceInfo.getAppName());

                    action.accept(instanceInfo);
                    executorService.shutdownNow();
                } catch (InstanceNotFoundException | RetryException e) {
                    log.debug(e.getMessage());
                    handleFailureConsumer.accept(e, false);
                } catch (Exception e) {
                    executorService.shutdownNow();
                    handleFailureConsumer.accept(e, true);
                    log.debug("Unexpected exception while retrieving {} service from Eureka", serviceId);
                }

            },
            INITIAL_DELAY, PERIOD, TimeUnit.MILLISECONDS
        );
    }
}
