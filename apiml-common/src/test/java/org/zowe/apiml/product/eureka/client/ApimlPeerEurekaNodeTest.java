/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.eureka.client;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.DefaultEurekaServerConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.cluster.protocol.ReplicationInstance;
import com.netflix.eureka.cluster.protocol.ReplicationList;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.ASGResource.ASGStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus;
import static com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl.Action;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.product.eureka.client.TestableHttpReplicationClient.RequestType;

class ApimlPeerEurekaNodeTest {
    private static final int BATCH_SIZE = 10;
    private static final long MAX_BATCHING_DELAY_MS = 10;

    private final PeerAwareInstanceRegistry registry = mock(PeerAwareInstanceRegistry.class);

    private final TestableHttpReplicationClient httpReplicationClient = new TestableHttpReplicationClient();

    public static final InstanceInfo instanceInfo = new InstanceInfo("service1", "service1-localhost", "group1", "127.0.0.1", "sid", null, new InstanceInfo.PortWrapper(true, 1999), "localhost:1999/home", "stat", "/health", null, null, null, 1, null, "localhost", InstanceInfo.InstanceStatus.UP, null, null, null, true, null, null, null, null, null);
    private final InstanceInfo instanceInfo2 = new InstanceInfo("service2", "service2-localhost", "group1", "127.0.0.1", "sid", null, new InstanceInfo.PortWrapper(true, 1998), "localhost:1999/home", "stat", "/health", null, null, null, 1, null, "localhost", InstanceInfo.InstanceStatus.UP, null, null, null, true, null, null, null, null, null);
    private ApimlPeerEurekaNode peerEurekaNode;

    @BeforeEach
    void setUp() {
        httpReplicationClient.withNetworkStatusCode(200);
        httpReplicationClient.withBatchReply(200);
    }

    @AfterEach
    void tearDown() {
        if (peerEurekaNode != null) {
            peerEurekaNode.shutDown();
        }
    }

    @Test
    void testRegistrationBatchReplication() throws Exception {
        createPeerEurekaNode().register(instanceInfo);

        ReplicationInstance replicationInstance = expectSingleBatchRequest();
        assertThat(replicationInstance.getAction(), is(equalTo(Action.Register)));
    }

    @Test
    void testCancelBatchReplication() throws Exception {
        createPeerEurekaNode().cancel(instanceInfo.getAppName(), instanceInfo.getId());

        ReplicationInstance replicationInstance = expectSingleBatchRequest();
        assertThat(replicationInstance.getAction(), is(equalTo(Action.Cancel)));
    }

    @Nested
    class GivenHeartbeatBatch {

        @Test
        void givenBatchReplication_thenExpectSingleBatch() throws Throwable {
            createPeerEurekaNode().heartbeat(instanceInfo.getAppName(), instanceInfo.getId(), instanceInfo, null, false);

            ReplicationInstance replicationInstance = expectSingleBatchRequest();
            assertThat(replicationInstance.getAction(), is(equalTo(Action.Heartbeat)));
        }

        @Test
        void givenReplicationFailure_thenScheduleSecondRegistration() throws Throwable {
            httpReplicationClient.withNetworkStatusCode(200, 200);
            httpReplicationClient.withBatchReply(404); // Not found, to trigger registration
            createPeerEurekaNode().heartbeat(instanceInfo.getAppName(), instanceInfo.getId(), instanceInfo, null, false);

            // Heartbeat replied with an error
            ReplicationInstance replicationInstance = expectSingleBatchRequest();
            assertThat(replicationInstance.getAction(), is(equalTo(Action.Heartbeat)));

            // Second, registration task is scheduled
            replicationInstance = expectSingleBatchRequest();
            assertThat(replicationInstance.getAction(), is(equalTo(Action.Register)));
        }

        @Test
        void givenInstanceInfoFromPeer_thenTriggerLocalCall() throws Throwable {
            InstanceInfo instanceInfoFromPeer = instanceInfo2;

            httpReplicationClient.withNetworkStatusCode(200);
            httpReplicationClient.withBatchReply(400);
            httpReplicationClient.withInstanceInfo(instanceInfoFromPeer);
            // InstanceInfo in response from peer will trigger local registry call
            createPeerEurekaNode().heartbeat(instanceInfo.getAppName(), instanceInfo.getId(), instanceInfo, null, false);
            expectRequestType(RequestType.Batch);

            // Check that registry has instanceInfo from peer
            verify(registry, timeout(1000).times(1)).register(instanceInfoFromPeer, true);
        }

    }

    @Test
    void testAsgStatusUpdate() throws Throwable {
        createPeerEurekaNode().statusUpdate(instanceInfo.getASGName(), ASGStatus.DISABLED);

        Object newAsgStatus = expectRequestType(RequestType.AsgStatusUpdate);
        assertThat(newAsgStatus, is(equalTo((Object) ASGStatus.DISABLED)));
    }

    @Test
    void testStatusUpdateBatchReplication() throws Throwable {
        createPeerEurekaNode().statusUpdate(instanceInfo.getAppName(), instanceInfo.getId(), InstanceStatus.DOWN, instanceInfo);

        ReplicationInstance replicationInstance = expectSingleBatchRequest();
        assertThat(replicationInstance.getAction(), is(equalTo(Action.StatusUpdate)));
    }

    @Test
    void testDeleteStatusOverrideBatchReplication() throws Throwable {
        createPeerEurekaNode().deleteStatusOverride(instanceInfo.getAppName(), instanceInfo.getId(), instanceInfo);

        ReplicationInstance replicationInstance = expectSingleBatchRequest();
        assertThat(replicationInstance.getAction(), is(equalTo(Action.DeleteStatusOverride)));
    }

    private ApimlPeerEurekaNode createPeerEurekaNode() {
        EurekaServerConfig config = new DefaultEurekaServerConfig("apiml");

        peerEurekaNode = new ApimlPeerEurekaNode(
            registry, "test", "http://test.host.com",
            httpReplicationClient,
            config,
            BATCH_SIZE,
            MAX_BATCHING_DELAY_MS,
            100,
            1000
        );
        return peerEurekaNode;
    }

    private Object expectRequestType(RequestType requestType) throws InterruptedException {
        TestableHttpReplicationClient.HandledRequest handledRequest = httpReplicationClient.nextHandledRequest(60, TimeUnit.SECONDS);
        assertThat(handledRequest, is(notNullValue()));
        assertThat(handledRequest.getRequestType(), is(equalTo(requestType)));
        return handledRequest.getData();
    }

    private ReplicationInstance expectSingleBatchRequest() throws InterruptedException {
        TestableHttpReplicationClient.HandledRequest handledRequest = httpReplicationClient.nextHandledRequest(30, TimeUnit.SECONDS);
        assertThat(handledRequest, is(notNullValue()));
        assertThat(handledRequest.getRequestType(), is(equalTo(TestableHttpReplicationClient.RequestType.Batch)));

        Object data = handledRequest.getData();
        assertThat(data, is(instanceOf(ReplicationList.class)));

        List<ReplicationInstance> replications = ((ReplicationList) data).getReplicationList();
        assertThat(replications.size(), is(equalTo(1)));
        return replications.get(0);
    }
}
