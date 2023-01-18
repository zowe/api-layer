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
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl.Action;
import com.netflix.eureka.util.batcher.TaskProcessor.ProcessingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.zowe.apiml.product.eureka.client.ApimlPeerEurekaNode.ReplicationTaskProcessor;
import static org.zowe.apiml.product.eureka.client.TestableInstanceReplicationTask.ProcessingState;
import static org.zowe.apiml.product.eureka.client.TestableInstanceReplicationTask.aReplicationTask;

public class ReplicationTaskProcessorTest {

    private final TestableHttpReplicationClient replicationClient = new TestableHttpReplicationClient();

    private ReplicationTaskProcessor replicationTaskProcessor;

    @BeforeEach
    public void setUp() {
        replicationTaskProcessor = new ReplicationTaskProcessor("peerId#test", replicationClient);
    }

    @Nested
    class GivenNonBatchableTask {

        @Test
        public void whenStatusCodeOK_thenSetSuccess() {
            TestableInstanceReplicationTask task = aReplicationTask().withAction(Action.Heartbeat).withReplyStatusCode(200).build();
            ProcessingResult status = replicationTaskProcessor.process(task);
            assertThat(status, is(ProcessingResult.Success));
        }

        @Test
        public void whenNetworkProblem_thenSetCongestion() {
            TestableInstanceReplicationTask task = aReplicationTask().withAction(Action.Heartbeat).withReplyStatusCode(503).build();
            ProcessingResult status = replicationTaskProcessor.process(task);
            assertThat(status, is(ProcessingResult.Congestion));
            assertThat(task.getProcessingState(), is(TestableInstanceReplicationTask.ProcessingState.Pending));
        }

        @Test
        public void whenNetworkProblemRepeated_thenSetTransient() {
            TestableInstanceReplicationTask task = aReplicationTask().withAction(Action.Heartbeat).withNetworkFailures(1).build();
            ProcessingResult status = replicationTaskProcessor.process(task);
            assertThat(status, is(ProcessingResult.TransientError));
            assertThat(task.getProcessingState(), is(ProcessingState.Pending));
        }

        @Test
        public void whenSSLProblem_thenSetPermanent() {
            TestableInstanceReplicationTask task = aReplicationTask().withAction(Action.Heartbeat).withException(new SSLException("handshake error")).withNetworkFailures(1).build();
            ProcessingResult status = replicationTaskProcessor.process(task);
            assertThat(status, is(ProcessingResult.PermanentError));
        }

        @Test
        public void whenNonNetworkError_thenSetPermanent() {
            TestableInstanceReplicationTask task = aReplicationTask().withAction(Action.Heartbeat).withReplyStatusCode(406).build();
            ProcessingResult status = replicationTaskProcessor.process(task);
            assertThat(status, is(ProcessingResult.PermanentError));
            assertThat(task.getProcessingState(), is(ProcessingState.Failed));
        }
    }

    @Nested
    class GivenBatchableTask {

        @Test
        public void whenStatusCodeOK_thenSetSuccess() {
            TestableInstanceReplicationTask task = aReplicationTask().build();

            replicationClient.withBatchReply(200);
            replicationClient.withNetworkStatusCode(200);
            ProcessingResult status = replicationTaskProcessor.process(Collections.<ReplicationTask>singletonList(task));

            assertThat(status, is(ProcessingResult.Success));
            assertThat(task.getProcessingState(), is(ProcessingState.Finished));
        }

        @Test
        public void whenNetworkProblem_thenSetCongestion() {
            TestableInstanceReplicationTask task = aReplicationTask().build();

            replicationClient.withNetworkStatusCode(503);
            ProcessingResult status = replicationTaskProcessor.process(Collections.<ReplicationTask>singletonList(task));

            assertThat(status, is(ProcessingResult.Congestion));
            assertThat(task.getProcessingState(), is(ProcessingState.Pending));
        }

        @Test
        public void whenReadTimeout_thenSetCongestion() {
            TestableInstanceReplicationTask task = aReplicationTask().build();

            replicationClient.withReadtimeOut(1);
            ProcessingResult status = replicationTaskProcessor.process(Collections.<ReplicationTask>singletonList(task));

            assertThat(status, is(ProcessingResult.Congestion));
            assertThat(task.getProcessingState(), is(ProcessingState.Pending));
        }


        @Test
        public void whenNetworkProblemRepeated_thenSetTransient() {
            TestableInstanceReplicationTask task = aReplicationTask().build();

            replicationClient.withNetworkError(1);
            ProcessingResult status = replicationTaskProcessor.process(Collections.<ReplicationTask>singletonList(task));

            assertThat(status, is(ProcessingResult.TransientError));
            assertThat(task.getProcessingState(), is(ProcessingState.Pending));
        }

        @Test
        public void whenSSLProblem_thenSetPermanent() {
            TestableInstanceReplicationTask task = aReplicationTask().build();

            replicationClient.withNetworkError(1);
            replicationClient.withException(new SSLException("handshake error"));
            ProcessingResult status = replicationTaskProcessor.process(Collections.<ReplicationTask>singletonList(task));

            assertThat(status, is(ProcessingResult.PermanentError));
        }

        @Test
        public void whenNetworkOKAndBatchFailed_thenSetFailed() {
            TestableInstanceReplicationTask task = aReplicationTask().build();
            InstanceInfo instanceInfoFromPeer = ApimlPeerEurekaNodeTest.instanceInfo;

            replicationClient.withNetworkStatusCode(200);
            replicationClient.withBatchReply(400);
            replicationClient.withInstanceInfo(instanceInfoFromPeer);
            ProcessingResult status = replicationTaskProcessor.process(Collections.<ReplicationTask>singletonList(task));

            assertThat(status, is(ProcessingResult.Success));
            assertThat(task.getProcessingState(), is(ProcessingState.Failed));
        }
    }
}
