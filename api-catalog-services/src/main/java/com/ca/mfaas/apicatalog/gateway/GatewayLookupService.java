/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.gateway;

import com.ca.mfaas.apicatalog.instance.InstanceInitializationException;
import com.ca.mfaas.apicatalog.instance.InstanceRetrievalService;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.netflix.appinfo.InstanceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.*;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.URI;


@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayLookupService {

    private GatewayConfigProperties gatewayConfigProperties;

    private final RetryTemplate gatewayRetryTemplate;
    private final InstanceRetrievalService instanceRetrievalService;

    @PostConstruct
    public void init() {
        log.info("Gateway lookup service is initializing...");
        lookupGatewayParams();
    }

    /**
     * Try to lookup gateway information from eureka
     */
    private void lookupGatewayParams() {
        log.info("Lookup for gateway params...");
        gatewayConfigProperties = gatewayRetryTemplate.execute(context -> initializeGatewayParams());
    }

    private GatewayConfigProperties initializeGatewayParams() {
        try {
            String gatewayHomePage = getGatewayHomePage();
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
            throw new GatewayLookupException(msg, e);
        }
    }

    private String getGatewayHomePage() {
        try {
            InstanceInfo gatewayInstance = instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId());
            if (gatewayInstance == null) {
                String msg = "Gateway Instance not retrieved from Discovery Service, retrying...";
                log.warn(msg);
                throw new RetryException(msg);
            } else {
                return gatewayInstance.getHomePageUrl();
            }
        } catch (InstanceInitializationException exp) {
            throw new RetryException(exp.getMessage());
        }
    }

    public GatewayConfigProperties getGatewayConfigProperties() {
        return gatewayConfigProperties;
    }
}
