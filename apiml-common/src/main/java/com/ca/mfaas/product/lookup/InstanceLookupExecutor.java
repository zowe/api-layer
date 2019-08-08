/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.lookup;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class InstanceLookupExecutor {

    private final EurekaClient eurekaClient;
    private final ScheduledExecutorService executorService =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "InstanceLookupExecutor-Thread");
            thread.setDaemon(true);
            return thread;
        }
    );

    private int initialDelay = 100;
    private int period = 1000;


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
            throw new InstanceNotFoundException("No " + serviceId + " Instances registered within application in Discovery Client");
        }

        return appInstances.get(0);
    }


    public void run(String serviceId, Consumer<InstanceInfo> action) {
        log.debug("Started instance finder");

        executorService.scheduleAtFixedRate(
            () -> {
                try {
                    InstanceInfo instanceInfo = findEurekaInstance(serviceId);
                    log.debug("App founded {}", instanceInfo.getAppName());

                    action.accept(instanceInfo);
                    executorService.shutdownNow();
                } catch (InstanceNotFoundException e) {
                    log.debug(e.getMessage());
                } catch (Exception e) {
                    log.debug("Unexpected exception, when {} has been tried to get from Eureka", serviceId);
                }

            },
            initialDelay, period, TimeUnit.MILLISECONDS
        );
    }
}
