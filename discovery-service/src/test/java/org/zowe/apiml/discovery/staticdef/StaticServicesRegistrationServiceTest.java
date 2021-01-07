/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discovery.staticdef;

import org.zowe.apiml.discovery.metadata.MetadataDefaultsService;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StaticServicesRegistrationServiceTest {

    private PeerAwareInstanceRegistry mockRegistry;

    @BeforeEach
    void setUp() {
        mockRegistry = mock(PeerAwareInstanceRegistry.class);
        EurekaServerContext mockEurekaServerContext = mock(EurekaServerContext.class);
        when(mockEurekaServerContext.getRegistry()).thenReturn(mockRegistry);
        EurekaServerContextHolder.initialize(mockEurekaServerContext);
    }

    private StaticRegistrationResult createResult(InstanceInfo...instances) {
        StaticRegistrationResult out = new StaticRegistrationResult();
        out.getInstances().addAll(Arrays.asList(instances));
        return out;
    }

    @Test
    void testFindServicesInDirectoryNoFiles() throws URISyntaxException {
        EurekaServerContext mockEurekaServerContext = mock(EurekaServerContext.class);
        EurekaServerContextHolder.initialize(mockEurekaServerContext);
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();

        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
        String apiDefsDirectory = Paths.get(ClassLoader.getSystemResource("api-defs-empty/").toURI()).toAbsolutePath().toString();
        StaticRegistrationResult result = registrationService.registerServices(apiDefsDirectory);
        assertEquals(0, result.getInstances().size());
    }

    @Test
    void testFindServicesInDirectoryOneFile() throws URISyntaxException {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();

        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
        String apiDefsDirectory = Paths.get(ClassLoader.getSystemResource("api-defs/").toURI()).toAbsolutePath().toString();
        StaticRegistrationResult result = registrationService.registerServices(apiDefsDirectory);

        assertEquals(4, result.getInstances().size());
    }

    @Test
    void testGetStaticInstances() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());

        List<InstanceInfo> instances = registrationService.getStaticInstances();

        assertEquals(0, instances.size());
        verify(serviceDefinitionProcessor, times(0)).findStaticServicesData(any(String.class));
    }

    @Test
    void testGetStaticInstancesAfterRegister() {
        String directory = "directory";
        String service = "service";
        ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
        when(serviceDefinitionProcessor.findStaticServicesData(directory)).thenReturn(createResult(
            InstanceInfo.Builder.newBuilder().setAppName(service).build()));

        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
        registrationService.registerServices(directory);
        List<InstanceInfo> instances = registrationService.getStaticInstances();

        assertEquals(1, instances.size());
        assertEquals(service.toUpperCase(), instances.get(0).getAppName());
        verify(serviceDefinitionProcessor, times(1)).findStaticServicesData(directory);
    }

    @Test
    void testReloadServicesWithUnregisteringService() {
        String service = "service";
        ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
        InstanceInfo instance = InstanceInfo.Builder.newBuilder().setInstanceId(service).setAppName(service).build();

        when(serviceDefinitionProcessor.findStaticServicesData(null))
            .thenReturn(createResult(instance))
            .thenReturn(createResult());

        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
        registrationService.reloadServices();
        StaticRegistrationResult result = registrationService.reloadServices();

        assertThat(result.getRegisteredServices().contains(service), is(false));
        verify(serviceDefinitionProcessor, times(2)).findStaticServicesData(null);
        verify(mockRegistry, times(1)).cancel(instance.getAppName(), instance.getId(), false);
    }

    @Test
    void testReloadServicesWithAddingNewService() {
        String serviceA = "serviceA";
        String serviceB = "serviceB";
        ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
        InstanceInfo instanceA = InstanceInfo.Builder.newBuilder().setInstanceId(serviceA).setAppName(serviceA).build();
        InstanceInfo instanceB = InstanceInfo.Builder.newBuilder().setInstanceId(serviceB).setAppName(serviceB).build();
        when(serviceDefinitionProcessor.findStaticServicesData(null))
            .thenReturn(createResult(instanceA))
            .thenReturn(createResult(instanceA, instanceB));

        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
        registrationService.reloadServices();
        StaticRegistrationResult result = registrationService.reloadServices();

        assertThat(result.getRegisteredServices().contains(serviceA), is(true));
        assertThat(result.getRegisteredServices().contains(serviceB), is(true));
        verify(serviceDefinitionProcessor, times(2)).findStaticServicesData(null);
        verify(mockRegistry, times(0)).cancel(any(String.class), any(String.class), eq(false));
    }

    @Test
    void testRenewInstances() {
        String directory = "directory";
        String service = "service";
        InstanceInfo instance = InstanceInfo.Builder.newBuilder().setInstanceId(service).setAppName(service).build();
        ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
        when(serviceDefinitionProcessor.findStaticServicesData(directory)).thenReturn(createResult(instance));

        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
        registrationService.registerServices(directory);
        registrationService.renewInstances();

        verify(mockRegistry, times(1)).renew(instance.getAppName(), instance.getId(), false);
    }

}
