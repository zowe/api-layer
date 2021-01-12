/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.eurekaservice.client.impl;

import com.netflix.discovery.EurekaClientConfig;
import org.mockito.Mockito;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.EurekaClientConfigProvider;
import org.zowe.apiml.eurekaservice.client.EurekaClientProvider;
import org.zowe.apiml.eurekaservice.client.config.*;
import org.zowe.apiml.eurekaservice.client.util.ApiMediationServiceConfigReader;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.util.EurekaInstanceConfigCreator;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.exception.ServiceDefinitionException;
import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class ApiMediationClientImplTest {

    private static final char[] PASSWORD = "password".toCharArray();

    @Test
    void startEurekaClient() throws ServiceDefinitionException {
        ApiInfo apiInfo = new ApiInfo("org.zowe.enabler.java", "api/v1", "1.0.0", "https://localhost:10014/apicatalog/api-doc", null);
        Catalog catalogUiTile = new Catalog(new Catalog.Tile("cademoapps", "Sample API Mediation Layer Applications", "Applications which demonstrate how to make a service integrated to the API Mediation Layer ecosystem", "1.0.0"));
        Authentication authentication = new Authentication("bypass", null);
        Ssl ssl = new Ssl(false, false, "TLSv1.2", "localhost", PASSWORD,
            "../keystore/localhost/localhost.keystore.p12", PASSWORD, "PKCS12",
            "../keystore/localhost/localhost.truststore.p12", PASSWORD, "PKCS12");
        List<Route> routes = new ArrayList<>();
        Route apiRoute = new Route("api/v1", "/hellospring/api/v1");
        Route apiDocRoute = new Route("api/v1/api-doc", "/hellospring/api-doc");
        routes.add(apiRoute);
        routes.add(apiDocRoute);

        ApiMediationServiceConfig config = ApiMediationServiceConfig.builder()
            .apiInfo(Collections.singletonList(apiInfo))
            .catalog(catalogUiTile)
            .authentication(authentication)
            .routes(routes)
            .description("Example for exposing a Spring REST API")
            .title("Hello Spring REST API")
            .serviceId("service")
            .baseUrl("http://host:1000/service")
            .healthCheckRelativeUrl("")
            .homePageRelativeUrl("")
            .statusPageRelativeUrl("")
            .discoveryServiceUrls(Collections.singletonList("https://localhost:10011/eureka"))
            .ssl(ssl)
            .serviceIpAddress("127.0.0.1")
            .build();

        ApiMediationClient client = new ApiMediationClientImpl();
        client.register(config);

        assertNotNull(client.getEurekaClient());
        assertEquals("SERVICE", client.getEurekaClient().getApplicationInfoManager().getInfo().getAppName());
        assertEquals(InstanceInfo.InstanceStatus.UP, client.getEurekaClient().getApplicationInfoManager().getInfo().getStatus());
        assertTrue(client.getEurekaClient().getApplicationInfoManager().getInfo().getMetadata().containsKey("apiml.authentication.scheme"));
        assertFalse(client.getEurekaClient().getApplicationInfoManager().getInfo().getMetadata().containsKey("apiml.authentication.applid"));
        // ...
        client.unregister();
    }

    @Test
    void badBaseUrlFormat() throws ServiceDefinitionException {
        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/bad-baseurl-service-configuration.yml");

        // Try register the services - expecting to throw ServiceDefinitionException
        ApiMediationClient client = new ApiMediationClientImpl();
        assertThrows(MetadataValidationException.class, () -> client.register(config));
        client.unregister();
    }

    @Test
    // It just tests that the https base configuration won't throw any exception.
    void httpsBaseUrlFormat() throws ServiceDefinitionException {
        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/https-service-configuration.yml");

        EurekaClientProvider clientProvider = Mockito.mock(EurekaClientProvider.class);
        EurekaInstanceConfigCreator instanceConfigCreator = new EurekaInstanceConfigCreator();

        EurekaClientConfig clientConfig = new EurekaClientConfiguration(config);
        EurekaClientConfigProvider eurekaClientConfigProvider = Mockito.mock(ApiMlEurekaClientConfigProvider.class);
        when(eurekaClientConfigProvider.config(config)).thenReturn(clientConfig);

        ApiMediationClient client = new ApiMediationClientImpl(clientProvider, eurekaClientConfigProvider, instanceConfigCreator);

        client.register(config);

        verify(clientProvider).client(any(), any(), any());
    }

    @Test
    void badProtocolForBaseUrl() throws ServiceDefinitionException {
        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/bad-protocol-baseurl-service-configuration.yml");

        ApiMediationClient client = new ApiMediationClientImpl();
        assertThrows(MetadataValidationException.class, () -> client.register(config));
        client.unregister();
    }

    @Test
    void testInitializationServiceDefinitionException() throws ServiceDefinitionException {
        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/service-configuration.yml");
        config.setBaseUrl(null);

        ApiMediationClient client = new ApiMediationClientImpl();
        assertThrows(MetadataValidationException.class, () -> client.register(config));
        client.unregister();
    }

    @Test
    void testInitializationRuntimeException() throws ServiceDefinitionException {
        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/service-configuration.yml");
        config.setRoutes(null);

        ApiMediationClient client = new ApiMediationClientImpl();

        Exception exception = assertThrows(MetadataValidationException.class, () -> client.register(config));
        assertEquals("Routes configuration was not provided. Try to add apiml.service.routes section.", exception.getMessage());
        client.unregister();
    }

    @Test
    void testInitialization_InvalidDocumentationUrl() throws ServiceDefinitionException {
        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();

        ApiMediationServiceConfig config = apiMediationServiceConfigReader.buildConfiguration("/service-configuration.yml");
        config.getApiInfo().get(0).setDocumentationUrl("HTT//INVALID-URL");

        ApiMediationClient client = new ApiMediationClientImpl();

        Exception exception = assertThrows(ServiceDefinitionException.class, () -> client.register(config));
        assertThat(exception.getCause(), instanceOf(MetadataValidationException.class));
        client.unregister();
    }
}
