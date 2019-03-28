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

import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.product.constants.CoreService;
import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Initialize a {@link GatewayConfigProperties} from a homepage of the Gateway.
 */
@Slf4j
@Component
public class GatewayConfigInitializer {

    private final InstanceRetrievalService instanceRetrievalService;

    /**
     * Create a new instance.
     *
     * @param instanceRetrievalService to obtain the Gateway instance
     */
    public GatewayConfigInitializer(InstanceRetrievalService instanceRetrievalService) {
        this.instanceRetrievalService = instanceRetrievalService;
    }


    /**
     * Get {@link GatewayConfigProperties} of the Gateway.
     */
    @Retryable(
        value = {RetryException.class},
        exclude = GatewayConfigInitializerException.class,
        maxAttempts = 100,
        backoff = @Backoff(delayExpression = "#{${mfaas.service-registry.serviceFetchDelayInMillis}}"))
    public GatewayConfigProperties getGatewayConfigProperties() throws GatewayConfigInitializerException {
        log.info("Initializing Gateway configurations by discovery service");

        try {
            String gatewayHomePage = getGatewayHomePage();
            URI uri = new URI(gatewayHomePage);

            return GatewayConfigProperties.builder()
                .scheme(uri.getScheme())
                .hostname(uri.getHost() + ":" + uri.getPort())
                .build();
        } catch (URISyntaxException e) {
            String msg = "Gateway URL is incorrect.";
            log.warn(msg, e);
            throw new GatewayConfigInitializerException(msg, e);
        }
    }

    @Recover
    public void recover(RetryException e) {
        log.warn("Failed to initialise Gateway configurations");
    }

    /**
     * Get a homepage of the Gateway.
     */
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
        } catch (Exception exp) {
            throw new RetryException(exp.getMessage());
        }
    }
}
