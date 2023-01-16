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

import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl.Action;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

class TestableInstanceReplicationTask extends InstanceReplicationTask {

    public static final String APP_NAME = "testableReplicationTaskApp";

    public enum ProcessingState {
        Pending, Finished, Failed
    }

    private final int replyStatusCode;
    private final int networkFailuresRepeatCount;

    private final AtomicReference<ProcessingState> processingState = new AtomicReference<>(ProcessingState.Pending);

    private volatile int triggeredNetworkFailures;
    private Exception exception;

    TestableInstanceReplicationTask(String peerNodeName,
                                    String appName,
                                    String id,
                                    Action action,
                                    int replyStatusCode,
                                    int networkFailuresRepeatCount,
                                    Exception exception) {
        super(peerNodeName, action, appName, id);
        this.replyStatusCode = replyStatusCode;
        this.networkFailuresRepeatCount = networkFailuresRepeatCount;
        this.exception = exception;
    }

    @Override
    public EurekaHttpResponse<Void> execute() throws Throwable {
        if (triggeredNetworkFailures < networkFailuresRepeatCount) {
            triggeredNetworkFailures++;
            throw exception;
        }
        return EurekaHttpResponse.status(replyStatusCode);
    }

    @Override
    public void handleSuccess() {
        processingState.compareAndSet(ProcessingState.Pending, ProcessingState.Finished);
    }

    @Override
    public void handleFailure(int statusCode, Object responseEntity) {
        processingState.compareAndSet(ProcessingState.Pending, ProcessingState.Failed);
    }

    public ProcessingState getProcessingState() {
        return processingState.get();
    }

    public static TestableReplicationTaskBuilder aReplicationTask() {
        return new TestableReplicationTaskBuilder();
    }

    static class TestableReplicationTaskBuilder {

        private int autoId;

        private int replyStatusCode = 200;
        private Action action = Action.Heartbeat;
        private int networkFailuresRepeatCount;
        private Exception exception = new IOException("simulated network failure");

        public TestableReplicationTaskBuilder withReplyStatusCode(int replyStatusCode) {
            this.replyStatusCode = replyStatusCode;
            return this;
        }

        public TestableReplicationTaskBuilder withException(Exception exception) {
            this.exception = exception;
            return this;
        }

        public TestableReplicationTaskBuilder withAction(Action action) {
            this.action = action;
            return this;
        }

        public TestableReplicationTaskBuilder withNetworkFailures(int networkFailuresRepeatCount) {
            this.networkFailuresRepeatCount = networkFailuresRepeatCount;
            return this;
        }

        public TestableInstanceReplicationTask build() {
            return new TestableInstanceReplicationTask(
                "peerNodeName#test",
                APP_NAME,
                "id#" + autoId++,
                action,
                replyStatusCode,
                networkFailuresRepeatCount,
                exception
            );
        }
    }
}
