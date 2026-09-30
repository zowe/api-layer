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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.EurekaClientConfigProvider;
import org.zowe.apiml.eurekaservice.client.EurekaClientProvider;
import org.zowe.apiml.eurekaservice.client.config.*;
import org.zowe.apiml.eurekaservice.client.util.ApiMediationServiceConfigReader;
import org.zowe.apiml.eurekaservice.client.util.EurekaInstanceConfigCreator;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.product.zos.ZUtilDummy;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.client.RegistryTransport;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class ApiMediationClientImplTest {

    private static final char[] PASSWORD = "password".toCharArray();

    private EurekaClientConfigProvider eurekaClientConfigProvider;

    private static final String[] SSL_SYSTEM_ENVIRONMENT_VALUES = {
        "javax.net.ssl.keyStore",
        "javax.net.ssl.keyStorePassword",
        "javax.net.ssl.keyStoreType",
        "javax.net.ssl.trustStore",
        "javax.net.ssl.trustStorePassword",
        "javax.net.ssl.trustStoreType"
    };

    /**
     * Records what the client sent, without any network. The enabler's registration drives this through the same
     * {@link RegistryTransport} the HTTP transport implements, which is what makes the assertions below statements
     * about the registration itself rather than about Netflix's client, as they used to be.
     */
    static class RecordingTransport implements RegistryTransport {

        final List<ServiceInstance> registered = new ArrayList<>();
        final List<String> cancelled = new ArrayList<>();

        @Override
        public Applications fetchApplications() {
            return new Applications(List.of(), 0L, "UP_0_");
        }

        @Override
        public Applications fetchDelta() {
            return null;
        }

        @Override
        public void register(ServiceInstance instance) {
            registered.add(instance);
        }

        @Override
        public boolean renew(String appName, String instanceId) {
            return true;
        }

        @Override
        public void cancel(String appName, String instanceId) {
            cancelled.add(appName + "/" + instanceId);
        }

        @Override
        public void updateStatus(String appName, String instanceId, InstanceStatus status) {
            // not exercised by these tests
        }

    }

    ApiMediationServiceConfig getValidConfiguration() {
        ApiInfo apiInfo = new ApiInfo("org.zowe.enabler.java", "api/v1", "1.0.0", "https://localhost:10014/apicatalog/api-doc", null, null);
        Catalog catalogUiTile = new Catalog(new Catalog.Tile("cademoapps", "Sample API Mediation Layer Applications", "Applications which demonstrate how to make a service integrated to the API Mediation Layer ecosystem", "1.0.0"));
        Authentication authentication = new Authentication("bypass", null, null);
        Ssl ssl = new Ssl(false, false, false, "TLSv1.2", "localhost", PASSWORD,
            "../keystore/service/service.keystore.p12", PASSWORD, "PKCS12",
            "../keystore/service/service.truststore.p12", PASSWORD, "PKCS12");
        List<Route> routes = new ArrayList<>();
        Route apiRoute = new Route("api/v1", "/hellospring/api/v1");
        Route apiDocRoute = new Route("api/v1/api-doc", "/hellospring/api-doc");
        routes.add(apiRoute);
        routes.add(apiDocRoute);

        return ApiMediationServiceConfig.builder()
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
    }

    @AfterEach
    void assertSystemEnvironmentValues() {
        for (String env : SSL_SYSTEM_ENVIRONMENT_VALUES) {
            assertNull(System.getProperty(env));
        }
    }

    private ApiMediationClient registerWithRecordingTransport(ApiMediationServiceConfig config,
                                                             DefaultCustomMetadataHelper metadataHelper) throws ServiceDefinitionException {
        RecordingTransport transport = new RecordingTransport();
        EurekaClientProvider clientProvider = mock(EurekaClientProvider.class);
        when(clientProvider.client(any(), any())).thenAnswer(invocation ->
            new RegistryClient(transport, invocation.getArgument(1, ServiceInstance.class)));

        ApiMediationClient client = new ApiMediationClientImpl(
            clientProvider,
            new ApiMlEurekaClientConfigProvider(),
            new EurekaInstanceConfigCreator(),
            metadataHelper
        );
        client.register(config);
        return client;
    }

    @Test
    void startRegistryClient() throws ServiceDefinitionException {
        ApiMediationServiceConfig config = getValidConfiguration();
        RecordingTransport transport = new RecordingTransport();

        EurekaClientProvider clientProvider = mock(EurekaClientProvider.class);
        when(clientProvider.client(any(), any())).thenAnswer(invocation ->
            new RegistryClient(transport, invocation.getArgument(1, ServiceInstance.class)));

        ApiMediationClient client = new ApiMediationClientImpl(clientProvider);
        client.register(config);

        assertNotNull(client.getRegistryClient());
        assertTrue(client.isRegistered());

        assertEquals(1, transport.registered.size());
        ServiceInstance registered = transport.registered.get(0);
        assertEquals("SERVICE", registered.appName());
        assertEquals(InstanceStatus.UP, registered.status());
        assertTrue(registered.metadata().containsKey("apiml.authentication.scheme"));
        assertFalse(registered.metadata().containsKey("apiml.authentication.applid"));
        assertEquals("host:service:1000", registered.instanceId());
        // An empty homePageRelativeUrl means no home page, exactly as on the Eureka path
        assertNull(registered.homePageUrl());
        assertEquals("http://host:1000/service", registered.statusPageUrl());
        assertEquals(1000, registered.port().port());
        assertTrue(registered.port().enabled());
        // ...
        client.unregister();
        assertEquals("SERVICE/host:service:1000", transport.cancelled.get(0));
    }

    @Test
    void registerTwiceIsRejected() throws ServiceDefinitionException {
        ApiMediationServiceConfig config = getValidConfiguration();
        RecordingTransport transport = new RecordingTransport();

        EurekaClientProvider clientProvider = mock(EurekaClientProvider.class);
        when(clientProvider.client(any(), any())).thenAnswer(invocation ->
            new RegistryClient(transport, invocation.getArgument(1, ServiceInstance.class)));

        ApiMediationClient client = new ApiMediationClientImpl(clientProvider);
        client.register(config);
        assertThrows(ServiceDefinitionException.class, () -> client.register(config));
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

        RecordingTransport transport = new RecordingTransport();
        EurekaClientProvider clientProvider = mock(EurekaClientProvider.class);
        when(clientProvider.client(any(), any())).thenAnswer(invocation ->
            new RegistryClient(transport, invocation.getArgument(1, ServiceInstance.class)));
        EurekaClientConfigProvider clientConfigProvider = mock(ApiMlEurekaClientConfigProvider.class);
        when(clientConfigProvider.config(config)).thenReturn(new EurekaClientConfiguration(config));

        ApiMediationClient client = new ApiMediationClientImpl(clientProvider, clientConfigProvider, new EurekaInstanceConfigCreator());

        client.register(config);

        verify(clientProvider).client(any(), any());
        assertEquals(1, transport.registered.size());
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

    private ApiMediationClient createApiMediationClient(DefaultCustomMetadataHelper defaultCustomMetadataHelper) {
        eurekaClientConfigProvider = spy(ApiMlEurekaClientConfigProvider.class);
        return new ApiMediationClientImpl(
            new DiscoveryClientProvider(),
            eurekaClientConfigProvider,
            new EurekaInstanceConfigCreator(),
            defaultCustomMetadataHelper
        );
    }

    @Test
    void testGivenCustomMetadata_whenRegister_thenValueIsNotChanged() throws ServiceDefinitionException {
        ApiMediationServiceConfig config = getValidConfiguration();
        ApiMediationClient client = createApiMediationClient(new DefaultCustomMetadataHelper());

        config.setCustomMetadata(new HashMap<>(Collections.singletonMap("os.name", "OSX")));
        client.register(config);
        assertEquals("OSX", config.getCustomMetadata().get("os.name"));
        client.unregister();
    }

    private org.zowe.apiml.product.zos.ZUtil getZUtilZosValue() {
        org.zowe.apiml.product.zos.ZUtilDummy zutil = mock(ZUtilDummy.class);
        doReturn("jobId").when(zutil).getCurrentJobId();
        doReturn("jobName").when(zutil).getCurrentJobname();
        doReturn("userId").when(zutil).getCurrentUser();
        doReturn(12345).when(zutil).getPid();
        doReturn("sysname").when(zutil).substituteSystemSymbols("&SYSNAME.");
        doReturn("sysclone").when(zutil).substituteSystemSymbols("&SYSCLONE.");
        doReturn("sysplex").when(zutil).substituteSystemSymbols("&SYSPLEX.");
        return zutil;
    }

    private DefaultCustomMetadataHelper getDefaultCustomMetadataHelper(boolean mockZos) {
        return new DefaultCustomMetadataHelper() {
            {
                setZUtil(getZUtilZosValue());
            }

            @Override
            protected boolean isRunningOnZos() {
                return mockZos;
            }
        };
    }

    @Test
    void testGivenZos_whenRegister_thenDefaultMetadataAreFetched() throws ServiceDefinitionException {
        ApiMediationServiceConfig config = getValidConfiguration();

        RecordingTransport transport = new RecordingTransport();
        EurekaClientProvider clientProvider = mock(EurekaClientProvider.class);
        when(clientProvider.client(any(), any())).thenAnswer(invocation ->
            new RegistryClient(transport, invocation.getArgument(1, ServiceInstance.class)));

        ApiMediationClient client = new ApiMediationClientImpl(
            clientProvider,
            new ApiMlEurekaClientConfigProvider(),
            new EurekaInstanceConfigCreator(),
            getDefaultCustomMetadataHelper(true)
        );

        client.register(config);
        assertEquals(System.getProperty("os.name"), config.getCustomMetadata().get("os.name"));
        assertEquals("jobId", config.getCustomMetadata().get("zos.jobid"));
        assertEquals("jobName", config.getCustomMetadata().get("zos.jobname"));
        assertEquals("userId", config.getCustomMetadata().get("zos.userid"));
        assertEquals(12345, config.getCustomMetadata().get("zos.pid"));
        assertEquals("sysname", config.getCustomMetadata().get("zos.sysname"));
        assertEquals("sysclone", config.getCustomMetadata().get("zos.sysclone"));
        assertEquals("sysplex", config.getCustomMetadata().get("zos.sysplex"));

        assertTrue(transport.registered.get(0).metadata().containsKey("zos.jobid"));
        client.unregister();
    }

    @Test
    void testGivenNonZos_whenRegister_thenDefaultMetadataAreFetched() throws ServiceDefinitionException {
        ApiMediationServiceConfig config = getValidConfiguration();

        RecordingTransport transport = new RecordingTransport();
        EurekaClientProvider clientProvider = mock(EurekaClientProvider.class);
        when(clientProvider.client(any(), any())).thenAnswer(invocation ->
            new RegistryClient(transport, invocation.getArgument(1, ServiceInstance.class)));
        eurekaClientConfigProvider = spy(ApiMlEurekaClientConfigProvider.class);

        ApiMediationClient client = new ApiMediationClientImpl(
            clientProvider,
            eurekaClientConfigProvider,
            new EurekaInstanceConfigCreator(),
            getDefaultCustomMetadataHelper(false)
        );

        doAnswer(invocation -> {
            ApiMediationServiceConfig configArg = invocation.getArgument(0);
            assertEquals(System.getProperty("os.name"), configArg.getCustomMetadata().get("os.name"));
            return invocation.callRealMethod();
        }).when(eurekaClientConfigProvider).config(any());

        client.register(config);

        assertEquals(System.getProperty("os.name"), config.getCustomMetadata().get("os.name"));
        assertNull(config.getCustomMetadata().get("zos.jobid"));
        assertFalse(transport.registered.get(0).metadata().containsKey("zos.jobid"));
        client.unregister();
    }

}
