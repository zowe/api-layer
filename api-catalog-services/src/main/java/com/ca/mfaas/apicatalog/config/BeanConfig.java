/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.config;

import com.ca.mfaas.apicatalog.gateway.GatewayLookupRetryPolicy;
import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.ca.mfaas.product.routing.transform.TransformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * General configuration of the API Catalog.
 */
@Configuration
@Slf4j
public class BeanConfig {

    @Bean
    @Primary
    public ErrorService errorServiceCatalog(ErrorService errorService) {
        errorService.loadMessages("/messages.yml");
        return errorService;
    }

    @Bean
    public TransformService transformService(GatewayConfigProperties gatewayConfigProperties) {
        return new TransformService(gatewayConfigProperties);
    }

    @Bean
    public RetryTemplate gatewayRetryTemplate(
        @Value("${mfaas.service-registry.serviceFetchDelayInMillis}") int serviceFetchDelayInMillis
    ) {
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(serviceFetchDelayInMillis);

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(new GatewayLookupRetryPolicy());
        template.setBackOffPolicy(backOffPolicy);

        return template;
    }
}
