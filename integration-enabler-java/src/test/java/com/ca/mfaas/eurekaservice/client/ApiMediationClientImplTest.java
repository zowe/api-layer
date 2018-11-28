/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client;

import com.ca.mfaas.eurekaservice.client.config.ApiMediationServiceConfig;
import com.ca.mfaas.eurekaservice.client.config.Ssl;
import com.ca.mfaas.eurekaservice.client.impl.ApiMediationClientImpl;
import com.ca.mfaas.eurekaservice.client.util.ApiMediationServiceConfigReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ApiMediationClientImplTest {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void startEurekaClient() {
        Ssl ssl = new Ssl(false, "TLSv1.2", "localhost", "password",
            "../keystore/localhost/localhost.keystore.p12", "password", "PKCS12",
            "../keystore/localhost/localhost.truststore.p12","password", "PKCS12");
        ApiMediationClient client = new ApiMediationClientImpl();
        ApiMediationServiceConfig config = ApiMediationServiceConfig.builder()
            .serviceId("service")
            .baseUrl("http://host:1000/service")
            .healthCheckRelativeUrl("")
            .homePageRelativeUrl("")
            .statusPageRelativeUrl("")
            .discoveryServiceUrl("https://localhost:10011/eureka")
            .ssl(ssl)
            .build();

        client.register(config);
        client.unregister();
    }

    @Test
    public void badBaseUrlFormat() {
        String file = "/bad-baseurl-service-configuration.yml";
        ApiMediationClient client = new ApiMediationClientImpl();
        ApiMediationServiceConfig config = new ApiMediationServiceConfigReader(file).readConfiguration();
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("baseUrl: [localhost:10020/hellospring] is not valid URL");

        client.register(config);
        client.unregister();
    }

    @Test
    public void httpsBaseUrlFormat() {
        String file = "/https-service-configuration.yml";
        ApiMediationClient client = new ApiMediationClientImpl();
        ApiMediationServiceConfig config = new ApiMediationServiceConfigReader(file).readConfiguration();

        client.register(config);
        client.unregister();
    }

    @Test
    public void badProtocolForBaseUrl() {
        String file = "/bad-protocol-baseurl-service-configuration.yml";
        ApiMediationClient client = new ApiMediationClientImpl();
        ApiMediationServiceConfig config = new ApiMediationServiceConfigReader(file).readConfiguration();
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Invalid protocol for baseUrl property");

        client.register(config);
        client.unregister();
    }

}
