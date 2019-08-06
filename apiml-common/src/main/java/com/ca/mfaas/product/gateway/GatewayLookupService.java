/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.product.gateway;

import com.ca.mfaas.product.constants.CoreService;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.retry.RetryException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.net.URI;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class GatewayLookupService {

    private GatewayConfigProperties foundGatewayConfigProperties;
    private final RetryTemplate retryTemplate;

    private final EurekaClient eurekaClient;
    private final Timer startupTimer = new Timer();

    public GatewayLookupService(@Qualifier("eurekaClient") EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
        this.retryTemplate = new RetryTemplate();

        retryTemplate.setRetryPolicy(new AlwaysRetryPolicy());
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
    }


    @EventListener
    public void postContextStart(ApplicationReadyEvent event) {
        if(foundGatewayConfigProperties!=null){
            return;
        }
        startupTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                initialize();
            }
        }, 100);
    }

    private void initialize(){
        if(foundGatewayConfigProperties!=null){
            log.warn("GatewayLookupService is already initialized");
            return;
        }

        log.info("GatewayLookupService starting asynchronous initialization of Gateway configuration");

        foundGatewayConfigProperties = retryTemplate.execute(context -> findGateway());
        log.info("GatewayLookupService has been initialized with Gateway instance on url: " + foundGatewayConfigProperties.getScheme() + "://" + foundGatewayConfigProperties.getHostname());
    }

    private GatewayConfigProperties findGateway() {

        Application application = eurekaClient.getApplication(CoreService.GATEWAY.getServiceId());
        if (application == null) {
            throw new RetryException("No Gateway Application is registered in Discovery Client");
        }

        List<InstanceInfo> appInstances = application.getInstances();
        if (appInstances.isEmpty()) {
            throw new RetryException("No Gateway Instances registered within Gateway Application in Discovery Client");
        }

        InstanceInfo firstInstance = appInstances.get(0);

        try {
            String gatewayHomePage = firstInstance.getHomePageUrl();
            URI uri = new URI(gatewayHomePage);

            return GatewayConfigProperties.builder()
                .scheme(uri.getScheme())
                .hostname(uri.getHost() + ":" + uri.getPort())
                .build();
        } catch (RetryException e) {
            throw e;
        } catch (Exception e) {
            String msg = "An unexpected error occurred while retrieving Gateway instance from Discovery service";
            log.warn(msg, e);
            throw new RuntimeException(msg, e);
        }

    }

    public GatewayConfigProperties getGatewayInstance() {
        if(foundGatewayConfigProperties ==null) {
            throw new GatewayNotFoundException("No Gateway Instance is known at the moment");
        }
        return foundGatewayConfigProperties;
    }
}
