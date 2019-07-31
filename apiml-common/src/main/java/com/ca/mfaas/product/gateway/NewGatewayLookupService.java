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
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Service
@Slf4j
public class NewGatewayLookupService {

    private GatewayConfigProperties foundGatewayConfigProperties;
    private final RetryTemplate retryTemplate;

    private final EurekaClient eurekaClient;
    private final Timer startupTimer = new Timer();

    public NewGatewayLookupService(@Qualifier("eurekaClient") EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
        this.retryTemplate = new RetryTemplate();

        retryTemplate.setRetryPolicy(new AlwaysRetryPolicy());
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
    }


    @EventListener
    public void startUp(ApplicationReadyEvent event) {
        startupTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                init();
            }
        }, 100);
    }

    private void init(){
        log.info("Looking for GW");
       foundGatewayConfigProperties = retryTemplate.execute(context -> findGateway());
    }

    private GatewayConfigProperties findGateway() {
        log.info("Looking ...");

        Application application = eurekaClient.getApplication(CoreService.GATEWAY.getServiceId());
        if (application==null) {
            log.error("No Application");
            throw new RetryException("nan");
        }
        log.info("Found" + application.toString());

        List<InstanceInfo> appInstances = application.getInstances();
        if(appInstances.isEmpty()) {
            log.error("no Instance");
            throw new RetryException("nan");
        }

        InstanceInfo firstInstance=appInstances.get(0);
        log.info("Found" + firstInstance.toString());

        //return firstInstance;

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
            String msg = "An unexpected exception occurred when trying to retrieve Gateway instance from Discovery service";
            log.warn(msg, e);
            throw new RuntimeException(msg, e);
        }

    }

    public GatewayConfigProperties getGatewayInstance() {

        if(foundGatewayConfigProperties ==null) {
            //TODO meaningful exception here
            throw new GatewayNotFoundException("No Gateway Instance is known at the moment");
        }

        return foundGatewayConfigProperties;
    }


}
