/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.standalone;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.apicatalog.instance.InstanceInitializeService;
import org.zowe.apiml.apicatalog.services.cached.CachedApiDocService;
import org.zowe.apiml.apicatalog.services.cached.CachedProductFamilyService;
import org.zowe.apiml.apicatalog.services.cached.CachedServicesService;
import org.zowe.apiml.apicatalog.services.status.model.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.services.status.model.ApiVersionNotFoundException;
import org.zowe.apiml.apicatalog.swagger.api.AbstractApiDocService;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.routing.transform.TransformService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class StandaloneLoaderServiceTest {

    private StandaloneLoaderService standaloneLoaderService;

    @Nested
    class WhenInitializeCache {

        private CachedServicesService cachedServicesService;
        private CachedApiDocService cachedApiDocService;
        private String testData;

        @BeforeEach
        void init() throws IOException {
            GatewayClient gatewayClient = new GatewayClient(null);
            TransformService transformService = new TransformService(gatewayClient);
            cachedServicesService = new CachedServicesService();
            CachedProductFamilyService cachedProductFamilyService =
                new CachedProductFamilyService(cachedServicesService, transformService, 1000, null);
            InstanceInitializeService instanceInitializeService = new InstanceInitializeService(cachedProductFamilyService, cachedServicesService, null, null);
            StandaloneAPIDocRetrievalService standaloneAPIDocRetrievalService = new StandaloneAPIDocRetrievalService();
            cachedApiDocService = new CachedApiDocService(standaloneAPIDocRetrievalService, null);
            standaloneLoaderService = new StandaloneLoaderService(
                new ObjectMapper(),
                instanceInitializeService,
                cachedApiDocService,
                standaloneAPIDocRetrievalService,
                s -> {
                    AbstractApiDocService<Object, Object> abstractApiDocService = mock(AbstractApiDocService.class);
                    doReturn(s).when(abstractApiDocService).transformApiDoc(any(), any());
                    return abstractApiDocService;
                },
                ServiceAddress.builder().scheme("https").hostname("localhost:10014").build(),
                mock(ExampleService.class)
            );

            testData = setTestData("standalone/services");
            cachedApiDocService.resetCache();
        }

        @Test
        void thenContainerCacheIsPopulated() {
            assertNull(cachedServicesService.getAllCachedServices());

            standaloneLoaderService.initializeCache();

            assertEquals(3, cachedServicesService.getAllCachedServices().size());
            assertEquals("SERVICE1-INSTANCE1", cachedServicesService.getAllCachedServices().getRegisteredApplications("service1-instance1").getName());
            assertEquals("SERVICE1-INSTANCE2", cachedServicesService.getAllCachedServices().getRegisteredApplications("service1-instance2").getName());
            assertEquals("SERVICE2", cachedServicesService.getAllCachedServices().getRegisteredApplications("service2").getName());
        }

        @Test
        void thenApiDocCacheIsPopulated() throws IOException {
            String expectedService1Instance1apiDoc = readTestData("/apiDocs/service1-instance1_zowe v1.0.0_default.json");
            String expectedService1Instance2apiDoc = readTestData("/apiDocs/service1-instance2_zowe v1.0.0_default.json");
            String expectedService2apiDoc1 = readTestData("/apiDocs/service2_zowe v1.0.0_default.json");
            String expectedService2apiDoc2 = readTestData("/apiDocs/service2_zowe v2.0.0.json");

            assertThrows(ApiDocNotFoundException.class, () -> cachedApiDocService.getDefaultApiDocForService("service1-instance1"));
            assertThrows(ApiDocNotFoundException.class, () -> cachedApiDocService.getDefaultApiDocForService("service1-instance2"));
            assertThrows(ApiDocNotFoundException.class, () -> cachedApiDocService.getDefaultApiDocForService("service2"));

            assertThrows(ApiDocNotFoundException.class, () -> cachedApiDocService.getApiDocForService("service1-instance1", "zowe v1.0.0"));
            assertThrows(ApiDocNotFoundException.class, () -> cachedApiDocService.getApiDocForService("service1-instance2", "zowe v1.0.0"));
            assertThrows(ApiDocNotFoundException.class, () -> cachedApiDocService.getApiDocForService("service2", "zowe v1.0.0"));
            assertThrows(ApiDocNotFoundException.class, () -> cachedApiDocService.getApiDocForService("service2", "zowe v2.0.0"));

            standaloneLoaderService.initializeCache();

            assertEquals(expectedService1Instance1apiDoc, cachedApiDocService.getDefaultApiDocForService("service1-instance1"));
            assertEquals(expectedService1Instance2apiDoc, cachedApiDocService.getDefaultApiDocForService("service1-instance2"));
            assertEquals(expectedService2apiDoc1, cachedApiDocService.getDefaultApiDocForService("service2"));

            assertEquals(expectedService1Instance1apiDoc, cachedApiDocService.getApiDocForService("service1-instance1", "zowe v1.0.0"));
            assertEquals(expectedService1Instance2apiDoc, cachedApiDocService.getApiDocForService("service1-instance2", "zowe v1.0.0"));
            assertEquals(expectedService2apiDoc1, cachedApiDocService.getApiDocForService("service2", "zowe v1.0.0"));
            assertEquals(expectedService2apiDoc2, cachedApiDocService.getApiDocForService("service2", "zowe v2.0.0"));
        }

        @Test
        void thenVersionCacheIsPopulated() {
            assertThrows(ApiVersionNotFoundException.class, () -> cachedApiDocService.getDefaultApiVersionForService("service1-instance1"));
            assertThrows(ApiVersionNotFoundException.class, () -> cachedApiDocService.getDefaultApiVersionForService("service1-instance2"));
            assertThrows(ApiVersionNotFoundException.class, () -> cachedApiDocService.getDefaultApiVersionForService("service2"));

            assertThrows(ApiVersionNotFoundException.class, () -> cachedApiDocService.getApiVersionsForService("service1-instance1"));
            assertThrows(ApiVersionNotFoundException.class, () -> cachedApiDocService.getApiVersionsForService("service1-instance2"));
            assertThrows(ApiVersionNotFoundException.class, () -> cachedApiDocService.getApiVersionsForService("service2"));

            standaloneLoaderService.initializeCache();

            assertEquals("zowe v1.0.0", cachedApiDocService.getDefaultApiVersionForService("service1-instance1"));
            assertEquals("zowe v1.0.0", cachedApiDocService.getDefaultApiVersionForService("service1-instance2"));
            assertEquals("zowe v1.0.0", cachedApiDocService.getDefaultApiVersionForService("service2"));

            assertEquals(Collections.singletonList("zowe v1.0.0"), cachedApiDocService.getApiVersionsForService("service1-instance1"));
            assertEquals(Collections.singletonList("zowe v1.0.0"), cachedApiDocService.getApiVersionsForService("service1-instance2"));
            assertEquals(Arrays.asList("zowe v1.0.0", "zowe v2.0.0"), cachedApiDocService.getApiVersionsForService("service2"));
        }

        private String readTestData(String fileName) throws IOException {
            return IOUtils.toString(Files.newInputStream(new File(testData + fileName).toPath()), StandardCharsets.UTF_8);
        }

    }

    @Nested
    class thenNoException {

        @BeforeEach
        void init() {
            standaloneLoaderService =
                new StandaloneLoaderService(new ObjectMapper(), null, null, null, null, null, null);
        }

        @Test
        void givenDefinitionDirectoryNotExist_whenInitializeCache() {
            ReflectionTestUtils.setField(standaloneLoaderService, "servicesDirectory", "not-exists");

            assertDoesNotThrow(standaloneLoaderService::initializeCache);
        }

        @Test
        void givenApiDocInvalidName_whenInitializeCache() throws IOException {
            setTestData("standalone/invalid-apiDocName");

            assertDoesNotThrow(standaloneLoaderService::initializeCache);
        }

        @Test
        void givenInvalidAppJson_whenInitializeCache() throws IOException {
            setTestData("standalone/invalid-app");

            assertDoesNotThrow(standaloneLoaderService::initializeCache);
        }

    }

    private String setTestData(String data) throws IOException {
        String testData = new ClassPathResource(data).getFile().getAbsolutePath();
        ReflectionTestUtils.setField(standaloneLoaderService, "servicesDirectory", testData);

        return testData;
    }

}
