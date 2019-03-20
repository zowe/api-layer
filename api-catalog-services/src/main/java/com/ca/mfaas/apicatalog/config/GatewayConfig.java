package com.ca.mfaas.apicatalog.config;

import com.ca.mfaas.apicatalog.exceptions.GatewayConfigInitializerException;
import com.ca.mfaas.apicatalog.model.GatewayConfigProperties;
import com.ca.mfaas.apicatalog.services.initialisation.GatewayConfigInitializer;
import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.product.constants.CoreService;
import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class GatewayConfig {

    private final GatewayConfigInitializer gatewayConfigInitializer;

    public GatewayConfig(GatewayConfigInitializer gatewayConfigInitializer) {
        this.gatewayConfigInitializer = gatewayConfigInitializer;
    }


    @Bean
    public GatewayConfigProperties getGatewayConfigProperties() throws GatewayConfigInitializerException {
        return gatewayConfigInitializer.getGatewayConfigProperties();
    }
}
