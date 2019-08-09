/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.instance.lookup;


import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.instance.InstanceInitializationException;
import com.ca.mfaas.product.instance.InstanceNotFoundException;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InstanceLookupExecutorTest {

    private static final int INITIAL_DELAY = 1;
    private static final int PERIOD = 10;
    private static final String SERVICE_ID = CoreService.API_CATALOG.getServiceId();

    @Mock
    private EurekaClient eurekaClient;

    private InstanceLookupExecutor instanceLookupExecutor;
    private List<InstanceInfo> instances;

    private volatile boolean isRunning = true;
    private Exception lastException;
    private InstanceInfo lastInstancInfo;

    @Before
    public void setUp() {
        instanceLookupExecutor = new InstanceLookupExecutor(eurekaClient, INITIAL_DELAY, PERIOD);
        instances = Collections.singletonList(
            getInstance(SERVICE_ID));
    }


    @Test(timeout = 2000)
    public void testRun_whenNoApplicationRegisteredInDiscovery() {
        instanceLookupExecutor.run(
            SERVICE_ID, null,
            (exception, isStopped) -> {
                lastException = exception;
                isRunning = false;
            }
        );

        while (isRunning);

        assertNotNull(lastException);
        assertTrue(lastException instanceof InstanceNotFoundException);
        assertEquals("No " + SERVICE_ID + " Application is registered in Discovery Client",
            lastException.getMessage());
    }


    @Test(timeout = 2000)
    public void testRun_whenNoInstancesExistInDiscovery() {
        when(eurekaClient.getApplication(SERVICE_ID))
            .thenReturn(new Application(SERVICE_ID, Collections.emptyList()));

        instanceLookupExecutor.run(
            SERVICE_ID, null,
            (exception, isStopped) -> {
                lastException = exception;
                isRunning = false;
            }
        );

        while (isRunning);

        assertNotNull(lastException);
        assertTrue(lastException instanceof InstanceNotFoundException);
        assertEquals("No " + SERVICE_ID + " Instances registered within Application in Discovery Client",
            lastException.getMessage());
    }

    @Test(timeout = 2000)
    public void testRun_whenUnxpectedExceptionHappened() {
        when(eurekaClient.getApplication(SERVICE_ID))
            .thenThrow(new InstanceInitializationException("Unexpected Exception"));

        instanceLookupExecutor.run(
            SERVICE_ID, null,
            (exception, isStopped) -> {
                lastException = exception;
                isRunning = !isStopped;
            }
        );

        while (isRunning);

        assertNotNull(lastException);
        assertTrue(lastException instanceof InstanceInitializationException);
    }

    @Test(timeout = 2000)
    public void testRun_whenInstanceExistInDiscovery() {
        when(eurekaClient.getApplication(SERVICE_ID))
            .thenReturn(new Application(SERVICE_ID, instances));

        instanceLookupExecutor.run(
            SERVICE_ID,
            instanceInfo -> {
                lastInstancInfo = instanceInfo;
                isRunning = false;
            },null
        );

        while (isRunning);

        assertNull(lastException);
        assertNotNull(lastInstancInfo);
        assertEquals(instances.get(0), lastInstancInfo);
    }


    private InstanceInfo getInstance(String serviceId) {
        InstanceInfo instance = createInstance(
            serviceId,
            serviceId,
            InstanceInfo.InstanceStatus.UP,
            InstanceInfo.ActionType.ADDED,
            new HashMap<>());
        return instance;
    }

    public InstanceInfo createInstance(String serviceId, String instanceId,
                                       InstanceInfo.InstanceStatus status,
                                       InstanceInfo.ActionType actionType,
                                       HashMap<String, String> metadata) {
        return new InstanceInfo(instanceId, serviceId.toUpperCase(), null, "192.168.0.1", null,
            new InstanceInfo.PortWrapper(true, 9090), null, null, null, null, null, null, null, 0, null, "hostname",
            status, null, null, null, null, metadata, null, null, actionType, null);
    }
}
