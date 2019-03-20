package com.ca.mfaas.apicatalog.services.initialisation;

import com.ca.mfaas.apicatalog.exceptions.GatewayConfigInitializerException;
import com.ca.mfaas.apicatalog.model.GatewayConfigProperties;
import com.ca.mfaas.product.constants.CoreService;
import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class GatewayConfigInitializer {

    private final InstanceRetrievalService instanceRetrievalService;

    public GatewayConfigInitializer(InstanceRetrievalService instanceRetrievalService) {
        this.instanceRetrievalService = instanceRetrievalService;
    }


    @Retryable(
        value = {RetryException.class},
        exclude = GatewayConfigInitializerException.class,
        maxAttempts = 5,
        backoff = @Backoff(delayExpression = "#{${mfaas.service-registry.serviceFetchDelayInMillis}}"))
    public GatewayConfigProperties getGatewayConfigProperties() throws GatewayConfigInitializerException {
        try {
            String gatewayHomePage = getGatewayHomePage();
            URI uri = new URI(gatewayHomePage);

            return GatewayConfigProperties.builder()
                .scheme(uri.getScheme())
                .hostname(uri.getHost() + ":" + uri.getPort())
                .homePageUrl(gatewayHomePage)
                .build();
        } catch (URISyntaxException e) {
            String msg = "Gateway URL is incorrect.";
            log.warn(msg, e);
            throw new GatewayConfigInitializerException(msg, e);
        }
    }

    private String getGatewayHomePage(){
        InstanceInfo gatewayInstance = instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId());
        if (gatewayInstance == null) {
            String msg = "Gateway Instance not retrieved from Discovery Service, retrying...";
            log.warn(msg);
            throw new RetryException(msg);
        } else {
            return gatewayInstance.getHomePageUrl();
        }
    }
}
