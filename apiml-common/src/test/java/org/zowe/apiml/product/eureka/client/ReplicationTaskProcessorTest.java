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
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.zowe.apiml.product.eureka.client.ApimlPeerEurekaNode.ReplicationTaskProcessor;
import static org.zowe.apiml.product.eureka.client.TestableInstanceReplicationTask.ProcessingState;
import static org.zowe.apiml.product.eureka.client.TestableInstanceReplicationTask.aReplicationTask;

public class ReplicationTaskProcessorTest {

    private final TestableHttpReplicationClient replicationClient = new TestableHttpReplicationClient();

    private ReplicationTaskProcessor replicationTaskProcessor;

    private static final int DEFAULT_MAX_RETRIES = 10;

    @BeforeEach
    public void setUp() {
        replicationTaskProcessor = new ReplicationTaskProcessor("peerId#test", replicationClient, DEFAULT_MAX_RETRIES);
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

        @Test
        void whenNetworkProblemRepeatedMultipleTimes_thenSetPermanentAndRecoverWhenNetworkIsOk() {
            TestableInstanceReplicationTask task = aReplicationTask().withAction(Action.Heartbeat).withNetworkFailures(DEFAULT_MAX_RETRIES).build();

            // First network issue should cause TransientError
            ProcessingResult status = replicationTaskProcessor.process(task);
            assertThat(status, is(ProcessingResult.TransientError));

            IntStream.range(1, DEFAULT_MAX_RETRIES - 2).forEach(n -> replicationTaskProcessor.process(task));

            // 9th network issue should still cause TransientError
            status = replicationTaskProcessor.process(task);
            assertThat(status, is(ProcessingResult.TransientError));

            // 10th network issue should finally cause PermanentError
            status = replicationTaskProcessor.process(task);
            assertThat(status, is(ProcessingResult.PermanentError));

            // Recovered network should lead to Success
            status = replicationTaskProcessor.process(task);
            assertThat(status, is(ProcessingResult.Success));
        }

        @Test
        void whenNetworkProblemRepeatedMultipleTimes_thenResetCounterAfterSuccessfulConnection() {
            TestableInstanceReplicationTask task1 = aReplicationTask().withAction(Action.Heartbeat).withNetworkFailures(DEFAULT_MAX_RETRIES - 5).build();
            TestableInstanceReplicationTask task2 = aReplicationTask().withAction(Action.Heartbeat).withNetworkFailures(DEFAULT_MAX_RETRIES).build();

            IntStream.range(1, DEFAULT_MAX_RETRIES - 5).forEach(n -> replicationTaskProcessor.process(task1));

            // 5th network issue should cause TransientError
            ProcessingResult status = replicationTaskProcessor.process(task1);
            assertThat(status, is(ProcessingResult.TransientError));

            // network issue counter is reset when network recovers
            status = replicationTaskProcessor.process(task1);
            assertThat(status, is(ProcessingResult.Success));

            IntStream.range(1, DEFAULT_MAX_RETRIES - 1).forEach(n -> replicationTaskProcessor.process(task2));

            // 9th network issue should cause TransientError since the counter was reset
            status = replicationTaskProcessor.process(task2);
            assertThat(status, is(ProcessingResult.TransientError));

            // 10th network issue should cause PermanentError
            status = replicationTaskProcessor.process(task2);
            assertThat(status, is(ProcessingResult.PermanentError));
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

        @Test
        void whenNetworkProblemRepeatedMultipleTimes_thenSetPermanentAndRecoverWhenNetworkIsOk() {
            TestableInstanceReplicationTask task = aReplicationTask().build();
            List<ReplicationTask> tasks = Collections.singletonList(task);
            replicationClient.withNetworkError(DEFAULT_MAX_RETRIES);

            // First network issue should cause TransientError
            ProcessingResult status = replicationTaskProcessor.process(tasks);
            assertThat(status, is(ProcessingResult.TransientError));

            IntStream.range(1, DEFAULT_MAX_RETRIES - 2).forEach(n -> replicationTaskProcessor.process(tasks));

            // 9th network issue should still cause TransientError
            status = replicationTaskProcessor.process(tasks);
            assertThat(status, is(ProcessingResult.TransientError));

            // 10th network issue should finally cause PermanentError
            status = replicationTaskProcessor.process(tasks);
            assertThat(status, is(ProcessingResult.PermanentError));

            replicationClient.withBatchReply(200);
            replicationClient.withNetworkStatusCode(200);

            // Recovered network should lead to Success
            status = replicationTaskProcessor.process(tasks);
            assertThat(status, is(ProcessingResult.Success));
        }

        @Test
        void whenNetworkProblemRepeatedMultipleTimes_thenResetCounterAfterSuccessfulConnection() {
            TestableInstanceReplicationTask task = aReplicationTask().build();
            List<ReplicationTask> tasks = Collections.singletonList(task);
            replicationClient.withNetworkError(DEFAULT_MAX_RETRIES - 5);

            IntStream.range(1, DEFAULT_MAX_RETRIES - 5).forEach(n -> replicationTaskProcessor.process(tasks));

            // 5th network issue should cause TransientError
            ProcessingResult status = replicationTaskProcessor.process(tasks);
            assertThat(status, is(ProcessingResult.TransientError));

            replicationClient.withBatchReply(200);
            replicationClient.withNetworkStatusCode(200);

            // network issue counter is reset when network recovers
            status = replicationTaskProcessor.process(tasks);
            assertThat(status, is(ProcessingResult.Success));

            replicationClient.withNetworkError(DEFAULT_MAX_RETRIES + 5);

            IntStream.range(1, DEFAULT_MAX_RETRIES - 1).forEach(n -> replicationTaskProcessor.process(tasks));

            // 9th network issue should cause TransientError since the counter was reset
            status = replicationTaskProcessor.process(tasks);
            assertThat(status, is(ProcessingResult.TransientError));

            // 10th network issue should cause PermanentError
            status = replicationTaskProcessor.process(tasks);
            assertThat(status, is(ProcessingResult.PermanentError));
        }
    }
}
