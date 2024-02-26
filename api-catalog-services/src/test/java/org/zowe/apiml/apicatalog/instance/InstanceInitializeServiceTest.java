/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.instance;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.Assertions;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.RetryException;
import org.zowe.apiml.apicatalog.services.cached.CachedProductFamilyService;
import org.zowe.apiml.apicatalog.services.cached.CachedServicesService;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.gateway.GatewayNotAvailableException;
import org.zowe.apiml.product.instance.InstanceInitializationException;
import org.zowe.apiml.product.registry.CannotRegisterServiceException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

@ExtendWith(MockitoExtension.class)
class InstanceInitializeServiceTest {

    @Mock
    private CachedServicesService cachedServicesService;

    @Mock
    private InstanceRetrievalService instanceRetrievalService;

    @Mock
    private CachedProductFamilyService cachedProductFamilyService;

    @SuppressWarnings("unused")
    @Mock
    private InstanceRefreshService instanceRefreshService;

    @InjectMocks
    private InstanceInitializeService instanceInitializeService;

    @Test
    void testRetrieveAndRegisterAllInstancesWithCatalog() throws CannotRegisterServiceException {
        Map<String, InstanceInfo> instanceInfoMap = createInstances();

        String catalogId = CoreService.API_CATALOG.getServiceId();
        InstanceInfo apiCatalogInstance = instanceInfoMap.get(catalogId.toUpperCase());
        when(
            instanceRetrievalService.getInstanceInfo(catalogId)
        ).thenReturn(apiCatalogInstance);

        Applications applications = new Applications();
        instanceInfoMap.values().forEach(f ->
            applications.addApplication(new Application(f.getAppName(), Collections.singletonList(f)))
        );

        when(
            instanceRetrievalService.getAllInstancesFromDiscovery(false)
        ).thenReturn(applications);


        instanceInitializeService.retrieveAndRegisterAllInstancesWithCatalog();

        verify(cachedProductFamilyService, times(2)).saveContainerFromInstance(
            apiCatalogInstance.getMetadata().get(CATALOG_ID),
            apiCatalogInstance
        );


        Optional<Application> catalogApplication = applications.getRegisteredApplications()
            .stream()
            .filter(f -> f.getName().equals(catalogId.toUpperCase()))
            .findFirst();

        Assertions.assertTrue(catalogApplication.isPresent());
        verify(cachedServicesService).updateService(catalogApplication.get().getName(), catalogApplication.get());


        instanceInfoMap.values()
            .stream()
            .filter(f -> !f.getAppName().equals(catalogId.toUpperCase()))
            .forEach(instanceInfo ->
                verify(cachedProductFamilyService, times(1)).saveContainerFromInstance(
                    instanceInfo.getMetadata().get(CATALOG_ID),
                    instanceInfo
                ));
    }

    @Test
    void shouldThrowExceptionWhenCatalogNotFound() throws CannotRegisterServiceException {
        String catalogId = CoreService.API_CATALOG.getServiceId();
        when(instanceRetrievalService.getInstanceInfo(catalogId)).thenReturn(null);

        Exception exception = Assertions.assertThrows(CannotRegisterServiceException.class, () -> {
            instanceInitializeService.retrieveAndRegisterAllInstancesWithCatalog();
        });
        Assertions.assertTrue(exception.getCause() instanceof RetryException);
    }

    @Test
    void shouldThrowRetryExceptionOnInstanceInitializationException() throws CannotRegisterServiceException {
        String catalogId = CoreService.API_CATALOG.getServiceId();
        when(instanceRetrievalService.getInstanceInfo(catalogId)).thenThrow(new InstanceInitializationException("ERROR"));

        Exception exception = Assertions.assertThrows(RetryException.class, () -> {
            instanceInitializeService.retrieveAndRegisterAllInstancesWithCatalog();
        });
        Assertions.assertEquals("ERROR", exception.getMessage());
    }

    @Test
    void shouldThrowRetryExceptionOnGatewayNotAvailableException() throws CannotRegisterServiceException {
        String catalogId = CoreService.API_CATALOG.getServiceId();
        when(instanceRetrievalService.getInstanceInfo(catalogId)).thenThrow(new GatewayNotAvailableException("ERROR"));

        Exception exception = Assertions.assertThrows(RetryException.class, () -> {
            instanceInitializeService.retrieveAndRegisterAllInstancesWithCatalog();
        });
        Assertions.assertEquals("ERROR", exception.getMessage());
    }

    private Map<String, InstanceInfo> createInstances() {
        Map<String, InstanceInfo> instanceInfoMap = new HashMap<>();

        InstanceInfo instanceInfo = getStandardInstance(
            CoreService.GATEWAY.getServiceId(),
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("apimediationlayer", "/" + CoreService.GATEWAY.getServiceId()),
            "gateway",
            "https://localhost:9090/");
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);

        instanceInfo = getStandardInstance(
            CoreService.API_CATALOG.getServiceId(),
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("apimediationlayer", "/" + CoreService.API_CATALOG.getServiceId()),
            "apicatalog",
            "https://localhost:9090/");
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);

        instanceInfo = getStandardInstance(
            "STATICCLIENT",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("static", "/discoverableclient"),
            "staticclient",
            "https://localhost:9090/discoverableclient");
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);

        instanceInfo = getStandardInstance(
            "STATICCLIENT2",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("static", "/discoverableclient"),
            "staticclient2",
            null);
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);

        instanceInfo = getStandardInstance(
            "ZOSMF1",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("zosmf", "/zosmf1"),
            "zosmf1",
            null);
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);

        instanceInfo = getStandardInstance(
            "ZOSMF2",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("zosmf", "/zosmf2"),
            "zosmf2",
            null);
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);

        return instanceInfoMap;
    }

    private HashMap<String, String> getMetadataByCatalogUiTitleId(String catalogUiTileId, String uiRoute) {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(CATALOG_ID, catalogUiTileId);
        metadata.put(ROUTES + ".ui-v1." + ROUTES_SERVICE_URL, uiRoute);
        metadata.put(ROUTES + ".ui-v1." + ROUTES_GATEWAY_URL, "ui/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "/");

        return metadata;
    }


    private InstanceInfo getStandardInstance(String serviceId,
                                             InstanceInfo.InstanceStatus status,
                                             HashMap<String, String> metadata,
                                             String vipAddress,
                                             String homePageUrl) {

        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(serviceId)
            .setAppName(serviceId)
            .setIPAddr("192.168.0.1")
            .enablePort(InstanceInfo.PortType.SECURE, true)
            .setSecurePort(9090)
            .setHostName("localhost")
            .setHomePageUrl(homePageUrl, homePageUrl)
            .setSecureVIPAddress("localhost")
            .setMetadata(metadata)
            .setVIPAddress(vipAddress)
            .setStatus(status)
            .build();
    }
}
