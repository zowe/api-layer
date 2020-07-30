/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zowe.apiml.client.model.DiscoverableClientConfig;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.config.Authentication;
import org.zowe.apiml.eurekaservice.client.config.Route;
import org.zowe.apiml.eurekaservice.client.config.Ssl;
import org.zowe.apiml.eurekaservice.client.impl.ApiMediationClientImpl;
import org.zowe.apiml.exception.ServiceDefinitionException;

import java.util.Collections;

/**
 * Service that allows a new {@link com.netflix.discovery.EurekaClient} to be registered and un-registered via ApiMediationClientImpl instance.
 * This service uses its own {@link com.netflix.discovery.EurekaClient} so that registration can be tested without affecting other services in
 * the Discoverable Client.
 */
@Service
public class ApiMediationClientService {
    private static final String PORT = "10013";
    private static final String SERVICE_ID = "registrationTest";
    private static final String GATEWAY_URL = "api/v1";

    private final DiscoverableClientConfig dcConfig;

    private final ApiMediationClient apiMediationClient;

    public ApiMediationClientService(@Autowired DiscoverableClientConfig dcConfig) {
        apiMediationClient = new ApiMediationClientImpl();
        this.dcConfig = dcConfig;
    }

    public boolean register() throws ServiceDefinitionException {
        ApiInfo apiInfo = new ApiInfo(SERVICE_ID, GATEWAY_URL, "1.0.0", null, null);
        Authentication authentication = new Authentication("bypass", null);
        Ssl ssl = new Ssl(dcConfig.isSslEnabled(), dcConfig.isVerifyCerts(), dcConfig.getSslProtocol(), dcConfig.getKeyAlias(),
            dcConfig.getKeyPassword().toCharArray(), dcConfig.getKeyStore(), dcConfig.getKeyStorePassword().toCharArray(),
            dcConfig.getKeyStoreType(), dcConfig.getTrustStore(), dcConfig.getTrustStorePassword().toCharArray(), dcConfig.getTrustStoreType());
        Route apiRoute = new Route(GATEWAY_URL, "/" + SERVICE_ID + "/" + GATEWAY_URL);

        ApiMediationServiceConfig apiConfig = ApiMediationServiceConfig.builder()
            .apiInfo(Collections.singletonList(apiInfo))
            .authentication(authentication)
            .routes(Collections.singletonList(apiRoute))
            .description("Example for API Mediation Client registration")
            .title("API Mediation Client Registration")
            .serviceId(SERVICE_ID)
            .baseUrl(dcConfig.getScheme() + "://" + dcConfig.getHostname() + ":" + PORT)
            .healthCheckRelativeUrl("")
            .homePageRelativeUrl("")
            .statusPageRelativeUrl("")
            .discoveryServiceUrls(dcConfig.getDiscoveryServiceUrls())
            .ssl(ssl)
            .preferIpAddress(false)
            .serviceIpAddress("0.0.0.0") //use hostname instead of IP address
            .build();
        apiMediationClient.register(apiConfig);
        return true; // indicates success, successful unless exception thrown. Used to assert success in unit tests.
    }

    public boolean unregister() {
        apiMediationClient.unregister();
        return true; // indicates success, successful unless exception thrown. Used to assert success in unit tests.
    }

    public boolean isRegistered() {
        return apiMediationClient.isRegistered();
    }
}
