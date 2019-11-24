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

import com.ca.mfaas.eurekaservice.client.config.*;
import com.ca.mfaas.eurekaservice.client.impl.ApiMediationClientImpl;
import com.ca.mfaas.eurekaservice.client.util.ApiMediationServiceConfigReader;
import com.ca.mfaas.config.ApiInfo;
import com.ca.mfaas.exception.ServiceDefinitionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;


public class ApiMediationClientImplTest {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void startEurekaClient() throws ServiceDefinitionException {
        ApiInfo apiInfo = new ApiInfo("org.zowe.enabler.java", "api/v1", "1.0.0", "https://localhost:10014/apicatalog/api-doc", null);
        Catalog catalogUiTile = new Catalog(new Catalog.Tile("cademoapps", "Sample API Mediation Layer Applications", "Applications which demonstrate how to make a service integrated to the API Mediation Layer ecosystem", "1.0.0"));
        Ssl ssl = new Ssl(false, false, "TLSv1.2", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
            "localhost", "password",
            "../keystore/localhost/localhost.keystore.p12", "password", "PKCS12",
            "../keystore/localhost/localhost.truststore.p12","password", "PKCS12");
        List<Route> routes = new ArrayList<Route>();
        Route apiRoute = new Route("api/v1", "/hellospring/api/v1");
        Route apiDocRoute = new Route("api/v1/api-doc", "/hellospring/api-doc");
        routes.add(apiRoute);
        routes.add(apiDocRoute);
        ApiMediationClient client = new ApiMediationClientImpl();
        ApiMediationServiceConfig config = ApiMediationServiceConfig.builder()
            .apiInfo(Collections.singletonList(apiInfo))
            .catalog(catalogUiTile)
            .routes(routes)
            .description("Example for exposing a Spring REST API")
            .title("Hello Spring REST API")
            .serviceId("service")
            .baseUrl("http://host:1000/service")
            .healthCheckRelativeUrl("")
            .homePageRelativeUrl("")
            .statusPageRelativeUrl("")
            .discoveryServiceUrl("https://localhost:10011/eureka")
            .ssl(ssl)
            .serviceIpAddress("127.0.0.1")
            .build();

        client.register(config);
        client.unregister();
    }

    @Test
    public void badBaseUrlFormat() throws ServiceDefinitionException {
        exceptionRule.expect(ServiceDefinitionException.class);

        String file = "/bad-baseurl-service-configuration.yml";
        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();
        String configData = apiMediationServiceConfigReader.readConfigurationFile(file);
        //exceptionRule.expectMessage("baseUrl: [localhost:10021/hellospring] is not valid URL");
        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfigurtion(configData);

        ApiMediationClient client = new ApiMediationClientImpl();
        client.register(config);
        client.unregister();
    }

    @Test
    public void httpsBaseUrlFormat() throws ServiceDefinitionException {
        //exceptionRule.expect( ServiceDefinitionException.class);

        String file = "/https-service-configuration.yml";
        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();
        String configData = apiMediationServiceConfigReader.readConfigurationFile(file);
        //exceptionRule.expectMessage("baseUrl: [localhost:10021/hellospring] is not valid URL");
        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfigurtion(configData);
        //ApiMediationServiceConfig config = new ApiMediationServiceConfigReader().readConfigurationFile(file);

        ApiMediationClient client = new ApiMediationClientImpl();
        client.register(config);
        client.unregister();
    }

    @Test
    public void badProtocolForBaseUrl() throws ServiceDefinitionException  {
        exceptionRule.expect( ServiceDefinitionException.class);

        String file = "/bad-protocol-baseurl-service-configuration.yml";
        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();
        String configData = apiMediationServiceConfigReader.readConfigurationFile(file);
        //exceptionRule.expectMessage("baseUrl: [localhost:10021/hellospring] is not valid URL");
        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfigurtion(configData);

        //ApiMediationServiceConfig config = new ApiMediationServiceConfigReader().readConfigurationFile(file);
        //exceptionRule.expectMessage("Invalid protocol for baseUrl property");

        ApiMediationClient client = new ApiMediationClientImpl();
        client.register(config);
        client.unregister();
    }
}
