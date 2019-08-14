/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.apicatalog.instance;

import com.ca.mfaas.apicatalog.services.cached.CachedProductFamilyService;
import com.ca.mfaas.apicatalog.services.cached.CachedServicesService;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.gateway.GatewayNotFoundException;
import com.ca.mfaas.product.instance.InstanceInitializationException;
import com.ca.mfaas.product.registry.CannotRegisterServiceException;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.retry.RetryException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.isA;

@RunWith(MockitoJUnitRunner.class)
public class InstanceInitializeServiceTest {

    @Mock
    private CachedServicesService cachedServicesService;

    @Mock
    private InstanceRetrievalService instanceRetrievalService;

    @Mock
    private CachedProductFamilyService cachedProductFamilyService;

    @InjectMocks
    private InstanceInitializeService instanceInitializeService;

    @Rule
    public final ExpectedException exception = ExpectedException.none();


    @Test
    public void testRetrieveAndRegisterAllInstancesWithCatalog() throws CannotRegisterServiceException {
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

        verify(cachedProductFamilyService, times(2)).createContainerFromInstance(
            apiCatalogInstance.getMetadata().get("mfaas.discovery.catalogUiTile.id"),
            apiCatalogInstance
        );


        Optional<Application> catalogApplication = applications.getRegisteredApplications()
            .stream()
            .filter(f -> f.getName().equals(catalogId.toUpperCase()))
            .findFirst();

        assertTrue(catalogApplication.isPresent());
        verify(cachedServicesService).updateService(catalogApplication.get().getName(), catalogApplication.get());


        instanceInfoMap.values()
            .stream()
            .filter(f -> !f.getAppName().equals(catalogId.toUpperCase()))
            .forEach(instanceInfo ->
                verify(cachedProductFamilyService, times(1)).createContainerFromInstance(
                    instanceInfo.getMetadata().get("mfaas.discovery.catalogUiTile.id"),
                    instanceInfo
                ));
    }

    @Test
    public void shouldThrowExceptionWhenCatalogNotFound() throws CannotRegisterServiceException {
        String catalogId = CoreService.API_CATALOG.getServiceId();
        when(instanceRetrievalService.getInstanceInfo(catalogId)).thenReturn(null);

        exception.expect(CannotRegisterServiceException.class);
        exception.expectCause(isA(RetryException.class));

        instanceInitializeService.retrieveAndRegisterAllInstancesWithCatalog();
    }

    @Test
    public void shouldThrowRetryExceptionOnInstanceInitializationException() throws CannotRegisterServiceException {
        String catalogId = CoreService.API_CATALOG.getServiceId();
        when(instanceRetrievalService.getInstanceInfo(catalogId)).thenThrow(new InstanceInitializationException("ERROR"));

        exception.expect(RetryException.class);
        exception.expectMessage("ERROR");

        instanceInitializeService.retrieveAndRegisterAllInstancesWithCatalog();
    }

    @Test
    public void shouldThrowRetryExceptionOnGatewayNotFoundException() throws CannotRegisterServiceException {
        String catalogId = CoreService.API_CATALOG.getServiceId();
        when(instanceRetrievalService.getInstanceInfo(catalogId)).thenThrow(new GatewayNotFoundException("ERROR"));

        exception.expect(RetryException.class);
        exception.expectMessage("ERROR");

        instanceInitializeService.retrieveAndRegisterAllInstancesWithCatalog();
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
            "ZOSMFTSO21",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("zosmf", "/zosmftso21"),
            "zosmftso21",
            null);
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);

        instanceInfo = getStandardInstance(
            "ZOSMFCA32",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("zosmf", "/zosmfca32"),
            "zosmfca32",
            null);
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);

        return instanceInfoMap;
    }

    private HashMap<String, String> getMetadataByCatalogUiTitleId(String catalogUiTileId, String uiRoute) {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("mfaas.discovery.catalogUiTile.id", catalogUiTileId);
        metadata.put("routed-services.ui-v1.service-url", uiRoute);
        metadata.put("routed-services.ui-v1.gateway-url", "ui/v1");
        metadata.put("routed-services.api-v1.gateway-url", "api/v1");
        metadata.put("routed-services.api-v1.service-url", "/");
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
