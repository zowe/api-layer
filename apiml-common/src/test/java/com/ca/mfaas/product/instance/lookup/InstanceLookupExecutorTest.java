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


//@RunWith(MockitoJUnitRunner.class)
public class InstanceLookupExecutorTest {

//    private static final int INITIAL_DELAY = 100;
//    private static final int PERIOD = 5000;
//
//    @Mock
//    private volatile EurekaClient eurekaClient;
//
//
//
//    @Rule
//    public final ExpectedException expectedException = ExpectedException.none();
//
//    private InstanceLookupExecutor instanceLookupExecutor;
//
//    private volatile boolean isRunning = true;
//
//    @Before
//    public void setUp() {
//        instanceLookupExecutor = new InstanceLookupExecutor(eurekaClient);
//    }
//
//
//    public void test() {
//
//    }
//
//
//    public void ds() {
//        String serviceId = CoreService.GATEWAY.getServiceId();
//
//        List<InstanceInfo> generatedInstances = Stream.iterate(0, i -> i + 1)
//            .limit(5)
//            .map(mIndex -> getInstance(mIndex, serviceId)).collect(Collectors.toList());
//
//
//        instanceLookupExecutor.run(
//            serviceId,
//            instanceInfo -> {
//                isRunning = false;
//            },
//            (exception, isStopped) -> {
//                assertTrue(exception instanceof InstanceNotFoundException);
//                assertEquals("No gateway Application is registered in Discovery Client",
//                    exception.getMessage());
//
//                when(eurekaClient.getApplication(serviceId))
//                    .thenReturn(new Application(serviceId, generatedInstances));
//
//
//                isRunning = !isStopped;
//            }
//        );
//
//        while (isRunning) ;
//    }
//
//    public void tesddtRun4() {
//        String serviceId = CoreService.GATEWAY.getServiceId();
//
//        List<InstanceInfo> generatedInstances = Stream.iterate(0, i -> i + 1)
//            .limit(5)
//            .map(mIndex -> getInstance(mIndex, serviceId)).collect(Collectors.toList());
//
//        when(eurekaClient.getApplication(serviceId))
//            .thenReturn(new Application(serviceId, Collections.emptyList()));
//
//        instanceLookupExecutor.run(
//            serviceId,
//            instanceInfo -> {
//                isRunning = false;
//            },
//            (exception, isStopped) -> {
//                exception.printStackTrace();
//                assertTrue(exception instanceof InstanceNotFoundException);
//                assertNotEquals("No gateway Instances registered within application in Discovery Client",
//                    exception.getMessage());
//
//                when(eurekaClient.getApplication(serviceId))
//                    .thenReturn(new Application(serviceId, generatedInstances));
//
//
//                isRunning = !isStopped;
//            }
//        );
//
//        while (isRunning) ;
//    }
//
//
//    private void testRegisteredApplicationInEureka_whenApplicationIsNotExist(Exception exception) {
//
//    }
//
//    private void testRegisteredApplicationInEureka_whenInstancesArentExist(String serviceId, Exception exception) {
//        when(eurekaClient.getApplication(serviceId))
//            .thenReturn(new Application(serviceId, null));
//
//        assertTrue(exception instanceof InstanceNotFoundException);
//        assertEquals("No gateway Application is registered in Discovery Client",
//            exception.getMessage());
//    }
//
//    private InstanceInfo getInstance(int index, String serviceId) {
//        InstanceInfo instance = createInstance(
//            serviceId,
//            serviceId + ":" + index,
//            InstanceInfo.InstanceStatus.UP,
//            InstanceInfo.ActionType.ADDED,
//            new HashMap<>());
//        return instance;
//    }
//
//    public InstanceInfo createInstance(String serviceId, String instanceId,
//                                       InstanceInfo.InstanceStatus status,
//                                       InstanceInfo.ActionType actionType,
//                                       HashMap<String, String> metadata) {
//        return new InstanceInfo(instanceId, serviceId.toUpperCase(), null, "192.168.0.1", null,
//            new InstanceInfo.PortWrapper(true, 9090), null, null, null, null, null, null, null, 0, null, "hostname",
//            status, null, null, null, null, metadata, null, null, actionType, null);
//    }
}
