/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.impl;

import com.ca.mfaas.eurekaservice.client.ApiMediationClient;
import com.ca.mfaas.eurekaservice.client.config.*;
import com.ca.mfaas.eurekaservice.client.util.ApiMediationServiceConfigReader;
import com.ca.mfaas.config.ApiInfo;
import com.ca.mfaas.exception.MetadataValidationException;
import com.ca.mfaas.exception.ServiceDefinitionException;
import com.netflix.appinfo.InstanceInfo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.hamcrest.core.Is.isA;
import static org.junit.Assert.*;


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
        ApiMediationServiceConfig config = new ApiMediationServiceConfig();
        config.setApiInfo(Collections.singletonList(apiInfo));
        config.setCatalog(catalogUiTile);
        config.setRoutes(routes);
        config.setDescription("Example for exposing a Spring REST API");
        config.setTitle("Hello Spring REST API");
        config.setServiceId("service");
        config.setBaseUrl("http://host:1000/service");
        config.setHealthCheckRelativeUrl("");
        config.setHomePageRelativeUrl("");
        config.setStatusPageRelativeUrl("");
        config.setDiscoveryServiceUrls(Arrays.asList("https://localhost:10011/eureka"));
        config.setSsl(ssl);
        config.setServiceIpAddress("127.0.0.1");
        //config.build();

        client.register(config);
        assertNotNull(client.getEurekaClient());
        assertEquals("SERVICE", client.getEurekaClient().getApplicationInfoManager().getInfo().getAppName());
        assertEquals(InstanceInfo.InstanceStatus.UP, client.getEurekaClient().getApplicationInfoManager().getInfo().getStatus());
        // ...
        client.unregister();
    }

    @Test
    public void badBaseUrlFormat() throws ServiceDefinitionException {
        exceptionRule.expect(ServiceDefinitionException.class);

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/bad-baseurl-service-configuration.yml");

        // Try register the services - expecting to throw ServiceDefinitionException
        ApiMediationClient client = new ApiMediationClientImpl();
        client.register(config);
        client.unregister();
    }

    @Test
    public void sslDisabledNoTrustStoreNoKeyStoreNeeded() throws ServiceDefinitionException {
        //exceptionRule.expect(ServiceDefinitionException.class);

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/https-disabled-service-configuration.yml");

        // Try register the services - expecting to throw ServiceDefinitionException
        ApiMediationClient client = new ApiMediationClientImpl();
        //client.getEurekaClient().getEurekaClientConfig()
        client.register(config);
        client.unregister();

        config.getSsl().setVerifySslCertificatesOfServices(false);
        client.register(config);
        client.unregister();
    }

    @Test
    public void httpsBaseUrlFormat() throws ServiceDefinitionException {
        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/https-service-configuration.yml");

        ApiMediationClient client = new ApiMediationClientImpl();
        client.register(config);
        assertNotNull(client.getEurekaClient());

        client.unregister();
        assertNull(client.getEurekaClient());
    }

    @Test
    public void badProtocolForBaseUrl() throws ServiceDefinitionException  {
        exceptionRule.expect( ServiceDefinitionException.class);

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/bad-protocol-baseurl-service-configuration.yml");

        ApiMediationClient client = new ApiMediationClientImpl();
        client.register(config);
        client.unregister();
    }

    @Test
    public void testMultipleSubsequentRegistrations() throws ServiceDefinitionException {
        exceptionRule.expect( ServiceDefinitionException.class);

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/service-configuration.yml");

        ApiMediationClient client = new ApiMediationClientImpl();

        // First registration attempt
        client.register(config);

        // Second registration attempt
        client.register(config);

        client.unregister();
    }

    @Test
    public void testInitializationServiceDefinitionException() throws ServiceDefinitionException {
        exceptionRule.expect( ServiceDefinitionException.class);

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/service-configuration.yml");
        config.setBaseUrl(null);

        ApiMediationClient client = new ApiMediationClientImpl();

        client.register(config);

        client.unregister();
    }

    @Test
    public void testInitializationRuntimeException() throws ServiceDefinitionException {
        exceptionRule.expect( ServiceDefinitionException.class);
        exceptionRule.expectCause(isA(NullPointerException.class));

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/service-configuration.yml");
        config.setRoutes(null);

        ApiMediationClient client = new ApiMediationClientImpl();

        client.register(config);

        client.unregister();
    }

    @Test
    public void testInitialization_InvalidDocumentationUrl() throws ServiceDefinitionException {
        exceptionRule.expect( ServiceDefinitionException.class);
        exceptionRule.expectCause(isA(MetadataValidationException.class));

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/service-configuration.yml");
        config.getApiInfo().get(0).setDocumentationUrl("HTT//INVALID-URL");

        ApiMediationClient client = new ApiMediationClientImpl();

        client.register(config);

        client.unregister();
    }
}
