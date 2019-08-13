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
import java.util.concurrent.CountDownLatch;

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

    private Exception lastException;
    private InstanceInfo lastInstanceInfo;
    private CountDownLatch latch;

    @Before
    public void setUp() {
        instanceLookupExecutor = new InstanceLookupExecutor(eurekaClient, INITIAL_DELAY, PERIOD);
        instances = Collections.singletonList(
            getInstance(SERVICE_ID));
        latch = new CountDownLatch(1);
    }


    @Test(timeout = 2000)
    public void testRun_whenNoApplicationRegisteredInDiscovery() throws InterruptedException {
        instanceLookupExecutor.run(
            SERVICE_ID, null,
            (exception, isStopped) -> {
                lastException = exception;
                latch.countDown();
            }
        );

        latch.await();

        assertNotNull(lastException);
        assertTrue(lastException instanceof InstanceNotFoundException);
        assertEquals("Service '" + SERVICE_ID + "' is not registered to Discovery Service",
            lastException.getMessage());
    }


    @Test(timeout = 2000)
    public void testRun_whenNoInstancesExistInDiscovery() throws InterruptedException {
        when(eurekaClient.getApplication(SERVICE_ID))
            .thenReturn(new Application(SERVICE_ID, Collections.emptyList()));

        instanceLookupExecutor.run(
            SERVICE_ID, null,
            (exception, isStopped) -> {
                lastException = exception;
                latch.countDown();
            }
        );

        latch.await();

        assertNotNull(lastException);
        assertTrue(lastException instanceof InstanceNotFoundException);
        assertEquals("'" + SERVICE_ID + "' has no running instances registered to Discovery Service",
            lastException.getMessage());
    }

    @Test(timeout = 2000)
    public void testRun_whenUnexpectedExceptionHappened() throws InterruptedException {
        when(eurekaClient.getApplication(SERVICE_ID))
            .thenThrow(new InstanceInitializationException("Unexpected Exception"));

        instanceLookupExecutor.run(
            SERVICE_ID, null,
            (exception, isStopped) -> {
                lastException = exception;
                latch.countDown();
            }
        );

        latch.await();

        assertNotNull(lastException);
        assertTrue(lastException instanceof InstanceInitializationException);
    }

    @Test(timeout = 2000)
    public void testRun_whenInstanceExistInDiscovery() throws InterruptedException {
        when(eurekaClient.getApplication(SERVICE_ID))
            .thenReturn(new Application(SERVICE_ID, instances));

        instanceLookupExecutor.run(
            SERVICE_ID,
            instanceInfo -> {
                lastInstanceInfo = instanceInfo;
                latch.countDown();
            }, null
        );

        latch.await();

        assertNull(lastException);
        assertNotNull(lastInstanceInfo);
        assertEquals(instances.get(0), lastInstanceInfo);
    }


    private InstanceInfo getInstance(String serviceId) {
        return createInstance(
            serviceId,
            serviceId,
            InstanceInfo.InstanceStatus.UP,
            InstanceInfo.ActionType.ADDED,
            new HashMap<>());
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
