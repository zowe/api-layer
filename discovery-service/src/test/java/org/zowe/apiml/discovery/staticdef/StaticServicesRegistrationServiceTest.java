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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StaticServicesRegistrationServiceTest {

    private PeerAwareInstanceRegistry mockRegistry;

    @Before
    public void setUp() {
        mockRegistry = mock(PeerAwareInstanceRegistry.class);
        EurekaServerContext mockEurekaServerContext = mock(EurekaServerContext.class);
        when(mockEurekaServerContext.getRegistry()).thenReturn(mockRegistry);
        EurekaServerContextHolder.initialize(mockEurekaServerContext);
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testFindServicesInDirectoryNoFiles() {
        EurekaServerContext mockEurekaServerContext = mock(EurekaServerContext.class);
        EurekaServerContextHolder.initialize(mockEurekaServerContext);
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();

        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor);
        Set<String> instances = registrationService.registerServices(folder.getRoot().getAbsolutePath());
        assertEquals(0, instances.size());
    }

    @Test
    public void testFindServicesInDirectoryOneFile() throws IOException, URISyntaxException {
        Files.copy(
            Paths.get(ClassLoader.getSystemResource("api-defs/staticclient.yml").toURI()).toAbsolutePath(),
            Paths.get(folder.getRoot().getAbsolutePath(), "test.yml")
        );

        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();

        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor);
        Set<String> instances = registrationService.registerServices(folder.getRoot().getAbsolutePath());

        assertEquals(1, instances.size());
    }

    @Test
    public void testGetStaticInstances() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor);

        List<InstanceInfo> instances = registrationService.getStaticInstances();

        assertEquals(0, instances.size());
        verify(serviceDefinitionProcessor, times(0)).findServices(any(String.class));
    }

    @Test
    public void testGetStaticInstancesAfterRegister() {
        String directory = "directory";
        String service = "service";
        ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
        when(serviceDefinitionProcessor.findServices(directory)).thenReturn(Arrays.asList(
            InstanceInfo.Builder.newBuilder().setAppName(service).build()));

        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor);
        registrationService.registerServices(directory);
        List<InstanceInfo> instances = registrationService.getStaticInstances();

        assertEquals(1, instances.size());
        assertEquals(service.toUpperCase(), instances.get(0).getAppName());
        verify(serviceDefinitionProcessor, times(1)).findServices(directory);
    }

    @Test
    public void testReloadServicesWithUnregisteringService() {
        String service = "service";
        ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
        InstanceInfo instance = InstanceInfo.Builder.newBuilder().setInstanceId(service).setAppName(service).build();
        when(serviceDefinitionProcessor.findServices(null))
            .thenReturn(Arrays.asList(instance))
            .thenReturn(Collections.emptyList());

        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor);
        registrationService.reloadServices();
        Set<String> services = registrationService.reloadServices();

        assertThat(services.contains(service), is(false));
        verify(serviceDefinitionProcessor, times(2)).findServices(null);
        verify(mockRegistry, times(1)).cancel(instance.getAppName(), instance.getId(), false);
    }

    @Test
    public void testReloadServicesWithAddingNewService() {
        String serviceA = "serviceA";
        String serviceB = "serviceB";
        ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
        InstanceInfo instanceA = InstanceInfo.Builder.newBuilder().setInstanceId(serviceA).setAppName(serviceA).build();
        InstanceInfo instanceB = InstanceInfo.Builder.newBuilder().setInstanceId(serviceB).setAppName(serviceB).build();
        when(serviceDefinitionProcessor.findServices(null))
            .thenReturn(Arrays.asList(instanceA))
            .thenReturn(Arrays.asList(instanceA, instanceB));

        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor);
        registrationService.reloadServices();
        Set<String> services = registrationService.reloadServices();

        assertThat(services.contains(serviceA), is(true));
        assertThat(services.contains(serviceB), is(true));
        verify(serviceDefinitionProcessor, times(2)).findServices(null);
        verify(mockRegistry, times(0)).cancel(any(String.class), any(String.class), eq(false));
    }

    @Test
    public void testRenewInstances() {
        String directory = "directory";
        String service = "service";
        InstanceInfo instance = InstanceInfo.Builder.newBuilder().setInstanceId(service).setAppName(service).build();
        ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
        when(serviceDefinitionProcessor.findServices(directory)).thenReturn(Arrays.asList(instance));

        StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor);
        registrationService.registerServices(directory);
        registrationService.renewInstances();

        verify(mockRegistry, times(1)).renew(instance.getAppName(), instance.getId(), false);
    }

}
