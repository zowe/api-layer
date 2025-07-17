/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/*
 * Copyright 2012 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.zowe.apiml.product.eureka.client;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.cluster.HttpReplicationClient;
import com.netflix.eureka.cluster.PeerEurekaNode;
import com.netflix.eureka.cluster.protocol.ReplicationInstance;
import com.netflix.eureka.cluster.protocol.ReplicationInstanceResponse;
import com.netflix.eureka.cluster.protocol.ReplicationList;
import com.netflix.eureka.cluster.protocol.ReplicationListResponse;
import com.netflix.eureka.lease.Lease;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import com.netflix.eureka.resources.ASGResource;
import com.netflix.eureka.util.batcher.TaskDispatcher;
import com.netflix.eureka.util.batcher.TaskDispatchers;
import com.netflix.eureka.util.batcher.TaskProcessor;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.netflix.eureka.cluster.protocol.ReplicationInstance.ReplicationInstanceBuilder.aReplicationInstance;

@Slf4j
public class ApimlPeerEurekaNode extends PeerEurekaNode {

    /**
     * A time to wait before continuing work if there is network level error.
     */
    private static final long RETRY_SLEEP_TIME_MS = 100;

    /**
     * A time to wait before continuing work if there is congestion on the server side.
     */
    private static final long SERVER_UNAVAILABLE_SLEEP_TIME_MS = 1000;

    /**
     * Maximum amount of time in ms to wait for new items prior to dispatching a batch of tasks.
     */
    private static final long MAX_BATCHING_DELAY_MS = 500;

    /**
     * Maximum batch size for batched requests.
     */
    private static final int BATCH_SIZE = 250;

    private final String serviceUrl;
    private final EurekaServerConfig config;
    private final long maxProcessingDelayMs;
    private final PeerAwareInstanceRegistry registry;
    private final String targetHost;
    private final HttpReplicationClient replicationClient;

    private final TaskDispatcher<String, ReplicationTask> batchingDispatcher;
    private final TaskDispatcher<String, ReplicationTask> nonBatchingDispatcher;

    public ApimlPeerEurekaNode(PeerAwareInstanceRegistry registry, String targetHost, String serviceUrl, HttpReplicationClient replicationClient, EurekaServerConfig config, int maxPeerRetries) {
        this(registry, targetHost, serviceUrl, replicationClient, config, BATCH_SIZE, MAX_BATCHING_DELAY_MS, RETRY_SLEEP_TIME_MS, SERVER_UNAVAILABLE_SLEEP_TIME_MS, maxPeerRetries);
    }

    /* For testing */ ApimlPeerEurekaNode(PeerAwareInstanceRegistry registry, String targetHost, String serviceUrl,
                                          HttpReplicationClient replicationClient, EurekaServerConfig config,
                                          int batchSize, long maxBatchingDelayMs,
                                          long retrySleepTimeMs, long serverUnavailableSleepTimeMs, int maxPeerRetries) {
        super(registry, targetHost, serviceUrl, replicationClient, config);
        this.registry = registry;
        this.targetHost = targetHost;
        this.replicationClient = replicationClient;

        this.serviceUrl = serviceUrl;
        this.config = config;
        this.maxProcessingDelayMs = config.getMaxTimeForReplication();

        String batcherName = getBatcherName();
        ReplicationTaskProcessor taskProcessor = new ReplicationTaskProcessor(targetHost, replicationClient, maxPeerRetries);
        this.batchingDispatcher = TaskDispatchers.createBatchingTaskDispatcher(
            batcherName,
            config.getMaxElementsInPeerReplicationPool(),
            batchSize,
            config.getMaxThreadsForPeerReplication(),
            maxBatchingDelayMs,
            serverUnavailableSleepTimeMs,
            retrySleepTimeMs,
            taskProcessor
        );
        this.nonBatchingDispatcher = TaskDispatchers.createNonBatchingTaskDispatcher(
            targetHost,
            config.getMaxElementsInStatusReplicationPool(),
            config.getMaxThreadsForStatusReplication(),
            maxBatchingDelayMs,
            serverUnavailableSleepTimeMs,
            retrySleepTimeMs,
            taskProcessor
        );
    }

    /**
     * Sends the registration information of {@link InstanceInfo} receiving by
     * this node to the peer node represented by this class.
     *
     * @param info the instance information {@link InstanceInfo} of any instance
     *             that is send to this instance.
     * @throws Exception
     */
    @Override
    public void register(final InstanceInfo info) throws Exception {
        long expiryTime = System.currentTimeMillis() + getLeaseRenewalOf(info);
        batchingDispatcher.process(
            taskId("register", info),
            new InstanceReplicationTask(targetHost, PeerAwareInstanceRegistryImpl.Action.Register, info, null, true) {
                public EurekaHttpResponse<Void> execute() {
                    return replicationClient.register(info);
                }
            },
            expiryTime
        );
    }

    /**
     * Send the cancellation information of an instance to the node represented
     * by this class.
     *
     * @param appName the application name of the instance.
     * @param id      the unique identifier of the instance.
     * @throws Exception
     */
    @Override
    public void cancel(final String appName, final String id) throws Exception {
        long expiryTime = System.currentTimeMillis() + maxProcessingDelayMs;
        batchingDispatcher.process(
            taskId("cancel", appName, id),
            new InstanceReplicationTask(targetHost, PeerAwareInstanceRegistryImpl.Action.Cancel, appName, id) {
                @Override
                public EurekaHttpResponse<Void> execute() {
                    return replicationClient.cancel(appName, id);
                }

                @Override
                public void handleFailure(int statusCode, Object responseEntity) throws Throwable {
                    super.handleFailure(statusCode, responseEntity);
                    if (statusCode == 404) {
                        log.warn("{}: missing entry.", getTaskName());
                    }
                }
            },
            expiryTime
        );
    }

    /**
     * Send the heartbeat information of an instance to the node represented by
     * this class. If the instance does not exist the node, the instance
     * registration information is sent again to the peer node.
     *
     * @param appName          the application name of the instance.
     * @param id               the unique identifier of the instance.
     * @param info             the instance info {@link InstanceInfo} of the instance.
     * @param overriddenStatus the overridden status information if any of the instance.
     * @throws Throwable
     */
    @Override
    public void heartbeat(final String appName, final String id,
                          final InstanceInfo info, final InstanceInfo.InstanceStatus overriddenStatus,
                          boolean primeConnection) throws Throwable {
        if (primeConnection) {
            // We do not care about the result for priming request.
            replicationClient.sendHeartBeat(appName, id, info, overriddenStatus);
            return;
        }
        ReplicationTask replicationTask = new InstanceReplicationTask(targetHost, PeerAwareInstanceRegistryImpl.Action.Heartbeat, info, overriddenStatus, false) {
            @Override
            public EurekaHttpResponse<InstanceInfo> execute() throws Throwable {
                return replicationClient.sendHeartBeat(appName, id, info, overriddenStatus);
            }

            @Override
            public void handleFailure(int statusCode, Object responseEntity) throws Throwable {
                super.handleFailure(statusCode, responseEntity);
                if (statusCode == 404) {
                    log.warn("{}: missing entry.", getTaskName());
                    if (info != null) {
                        log.warn("{}: cannot find instance id {} and hence replicating the instance with status {}",
                            getTaskName(), info.getId(), info.getStatus());
                        register(info);
                    }
                } else if (config.shouldSyncWhenTimestampDiffers()) {
                    InstanceInfo peerInstanceInfo = (InstanceInfo) responseEntity;
                    if (peerInstanceInfo != null) {
                        syncInstancesWhenTimestampDiffers(appName, id, info, peerInstanceInfo);
                    }
                }
            }
        };
        long expiryTime = System.currentTimeMillis() + getLeaseRenewalOf(info);
        log.debug("Heartbeat update");
        batchingDispatcher.process(taskId("heartbeat", info), replicationTask, expiryTime);
    }

    /**
     * Send the status information of of the ASG represented by the instance.
     *
     * <p>
     * ASG (Autoscaling group) names are available for instances in AWS and the
     * ASG information is used for determining if the instance should be
     * registered as {@link InstanceInfo.InstanceStatus#DOWN} or {@link InstanceInfo.InstanceStatus#UP}.
     *
     * @param asgName   the asg name if any of this instance.
     * @param newStatus the new status of the ASG.
     */
    @Override
    public void statusUpdate(final String asgName, final ASGResource.ASGStatus newStatus) {
        long expiryTime = System.currentTimeMillis() + maxProcessingDelayMs;
        nonBatchingDispatcher.process(
            asgName,
            new AsgReplicationTask(targetHost, PeerAwareInstanceRegistryImpl.Action.StatusUpdate, asgName, newStatus) {
                public EurekaHttpResponse<?> execute() {
                    return replicationClient.statusUpdate(asgName, newStatus);
                }
            },
            expiryTime
        );
    }

    /**
     * Send the status update of the instance.
     *
     * @param appName   the application name of the instance.
     * @param id        the unique identifier of the instance.
     * @param newStatus the new status of the instance.
     * @param info      the instance information of the instance.
     */
    @Override
    public void statusUpdate(final String appName, final String id,
                             final InstanceInfo.InstanceStatus newStatus, final InstanceInfo info) {
        long expiryTime = System.currentTimeMillis() + maxProcessingDelayMs;
        batchingDispatcher.process(
            taskId("statusUpdate", appName, id),
            new InstanceReplicationTask(targetHost, PeerAwareInstanceRegistryImpl.Action.StatusUpdate, info, null, false) {
                @Override
                public EurekaHttpResponse<Void> execute() {
                    return replicationClient.statusUpdate(appName, id, newStatus, info);
                }
            },
            expiryTime
        );
    }

    /**
     * Delete instance status override.
     *
     * @param appName the application name of the instance.
     * @param id      the unique identifier of the instance.
     * @param info    the instance information of the instance.
     */
    @Override
    public void deleteStatusOverride(final String appName, final String id, final InstanceInfo info) {
        long expiryTime = System.currentTimeMillis() + maxProcessingDelayMs;
        batchingDispatcher.process(
            taskId("deleteStatusOverride", appName, id),
            new InstanceReplicationTask(targetHost, PeerAwareInstanceRegistryImpl.Action.DeleteStatusOverride, info, null, false) {
                @Override
                public EurekaHttpResponse<Void> execute() {
                    return replicationClient.deleteStatusOverride(appName, id, info);
                }
            },
            expiryTime);
    }

    /**
     * Shuts down all resources used for peer replication.
     */
    @Override
    public void shutDown() {
        batchingDispatcher.shutdown();
        nonBatchingDispatcher.shutdown();
        replicationClient.shutdown();
    }

    /**
     * Synchronize {@link InstanceInfo} information if the timestamp between
     * this node and the peer eureka nodes vary.
     */
    private void syncInstancesWhenTimestampDiffers(String appName, String id, InstanceInfo info, InstanceInfo infoFromPeer) {
        try {
            log.warn("Peer wants us to take the instance information from it, since the timestamp differs,"
                + "Id : {} My Timestamp : {}, Peer's timestamp: {}", id, info.getLastDirtyTimestamp(), infoFromPeer.getLastDirtyTimestamp());

            if (infoFromPeer.getOverriddenStatus() != null && !InstanceInfo.InstanceStatus.UNKNOWN.equals(infoFromPeer.getOverriddenStatus())) {
                log.warn("Overridden Status info -id {}, mine {}, peer's {}", id, info.getOverriddenStatus(), infoFromPeer.getOverriddenStatus());
                registry.storeOverriddenStatusIfRequired(appName, id, infoFromPeer.getOverriddenStatus());
            }
            registry.register(infoFromPeer, true);
        } catch (Exception e) {
            log.warn("Exception when trying to set information from peer :", e);
        }
    }

    private static String taskId(String requestType, String appName, String id) {
        return requestType + '#' + appName + '/' + id;
    }

    private static String taskId(String requestType, InstanceInfo info) {
        return taskId(requestType, info.getAppName(), info.getId());
    }

    private static long getLeaseRenewalOf(InstanceInfo info) {
        return (info.getLeaseInfo() == null ? (long) Lease.DEFAULT_DURATION_IN_SECS : info.getLeaseInfo().getRenewalIntervalInSecs()) * 1000L;
    }

    @Slf4j
    public static class ReplicationTaskProcessor implements TaskProcessor<ReplicationTask> {

        private final HttpReplicationClient replicationClient;

        private final String peerId;

        private volatile long lastNetworkErrorTime;

        private static final Pattern READ_TIME_OUT_PATTERN = Pattern.compile(".*read.*time.*out.*");

        private final NetworkIssueCounter networkIssueCounter = new NetworkIssueCounter();

        private final int maxPeerRetries;

        public ReplicationTaskProcessor(String peerId, HttpReplicationClient replicationClient, int maxPeerRetries) {
            this.replicationClient = replicationClient;
            this.peerId = peerId;
            this.maxPeerRetries = maxPeerRetries;
        }

        class NetworkIssueCounter {

            final AtomicInteger counter = new AtomicInteger(0);

            private String getCountText() {
                int count = counter.get();

                StringBuilder sb = new StringBuilder();
                sb.append(count);
                if (count >= maxPeerRetries) sb.append('+');

                return sb.toString();
            }

            public void success() {
                int count = counter.get();
                if (count > 0) {
                    log.trace("Network error indicator was reset. The number of errors was {}/{}", getCountText(), maxPeerRetries);
                }
                counter.set(0);
            }

            public void fail(String errorMessage) {
                counter.getAndUpdate(prev -> Math.min(prev + 1, maxPeerRetries));
                log.trace("Network error ({}) occurred. The number of errors is {}/{}. The network error status is considered as {}.",
                    errorMessage, getCountText(),
                    maxPeerRetries, hasReachedMax() ? "permanent" : "temporary");
            }

            public boolean hasReachedMax() {
                return counter.get() >= maxPeerRetries;
            }

        }

        @Override
        public ProcessingResult process(ReplicationTask task) {
            try {
                EurekaHttpResponse<?> httpResponse = task.execute();
                int statusCode = httpResponse.getStatusCode();
                Object entity = httpResponse.getEntity();

                log.debug("Replication task {} completed with status {}, (includes entity {})", task.getTaskName(), statusCode, entity != null);

                if (isSuccess(statusCode)) {
                    networkIssueCounter.success();
                    task.handleSuccess();
                } else if (statusCode == 503) {
                    networkIssueCounter.fail("Service is not available");
                    log.debug("Server busy (503) reply for task {}", task.getTaskName());
                    return ProcessingResult.Congestion;
                } else {
                    task.handleFailure(statusCode, entity);
                    return ProcessingResult.PermanentError;
                }
            } catch (Throwable e) {
                networkIssueCounter.fail(e.getLocalizedMessage());
                if (maybeReadTimeOut(e)) {
                    log.error("It seems to be a socket read timeout exception, it will retry later. if it continues to happen and some eureka node occupied all the cpu time, you should set property 'eureka.server.peer-node-read-timeout-ms' to a bigger value", e);
                    //read timeout exception is more Congestion than TransientError, return Congestion for longer delay
                    return ProcessingResult.Congestion;
                } else if (isNetworkConnectException(e) && !networkIssueCounter.hasReachedMax()) {
                    logNetworkErrorSample(task, "; retrying after delay.", e);
                    return ProcessingResult.TransientError;
                } else {
                    logNetworkErrorSample(task, "; not re-trying this exception because it does not seem to be a network exception.", e);
                    return ProcessingResult.PermanentError;
                }
            }
            return ProcessingResult.Success;
        }

        @Override
        public ProcessingResult process(List<ReplicationTask> tasks) {
            ReplicationList list = createReplicationListOf(tasks);
            try {
                EurekaHttpResponse<ReplicationListResponse> response = replicationClient.submitBatchUpdates(list);
                int statusCode = response.getStatusCode();
                if (!isSuccess(statusCode)) {
                    if (statusCode == 503) {
                        networkIssueCounter.fail("Service is not available");
                        log.warn("Server busy (503) HTTP status code received from the peer {}; rescheduling tasks after delay", peerId);
                        return ProcessingResult.Congestion;
                    } else {
                        // Unexpected error returned from the server. This should ideally never happen.
                        log.error("Batch update failure with HTTP status code {}; discarding {} replication tasks", statusCode, tasks.size());
                        return ProcessingResult.PermanentError;
                    }
                } else {
                    networkIssueCounter.success();
                    handleBatchResponse(tasks, response.getEntity().getResponseList());
                }
            } catch (Throwable e) {
                networkIssueCounter.fail(e.getLocalizedMessage());
                if (maybeReadTimeOut(e)) {
                    log.error("It seems to be a socket read timeout exception, it will retry later. if it continues to happen and some eureka node occupied all the cpu time, you should set property 'eureka.server.peer-node-read-timeout-ms' to a bigger value", e);
                    //read timeout exception is more Congestion than TransientError, return Congestion for longer delay
                    return ProcessingResult.Congestion;
                } else if (isNetworkConnectException(e) && !networkIssueCounter.hasReachedMax()) {
                    logNetworkErrorSample(null, "; retrying after delay.", e);
                    return ProcessingResult.TransientError;
                } else {
                    logNetworkErrorSample(null, "; not re-trying this exception because it does not seem to be a network exception.", e);
                    return ProcessingResult.PermanentError;
                }
            }
            return ProcessingResult.Success;
        }

        /**
         * We want to retry eagerly, but without flooding log file with tons of error entries.
         * As tasks are executed by a pool of threads the error logging multiplies. For example:
         * 20 threads * 100ms delay == 200 error entries / sec worst case
         * Still we would like to see the exception samples, so we print samples at regular intervals.
         */
        private void logNetworkErrorSample(ReplicationTask task, String additionalMessage, Throwable e) {
            long messageTimeout = 10000;
            long now = System.currentTimeMillis();
            if (now - lastNetworkErrorTime > messageTimeout) {
                lastNetworkErrorTime = now;
                StringBuilder sb = new StringBuilder();
                sb.append("Network level connection to peer ").append(peerId);
                if (task != null) {
                    sb.append(" for task ").append(task.getTaskName());
                }
                sb.append(additionalMessage);
                sb.append(" This message will suppressed for ").append(messageTimeout).append("ms.");
                log.error(sb.toString(), e);
            }
        }

        private void handleBatchResponse(List<ReplicationTask> tasks, List<ReplicationInstanceResponse> responseList) {
            if (tasks.size() != responseList.size()) {
                // This should ideally never happen unless there is a bug in the software.
                log.error("Batch response size different from submitted task list ({} != {}); skipping response analysis", responseList.size(), tasks.size());
                return;
            }
            for (int i = 0; i < tasks.size(); i++) {
                handleBatchResponse(tasks.get(i), responseList.get(i));
            }
        }

        private void handleBatchResponse(ReplicationTask task, ReplicationInstanceResponse response) {
            int statusCode = response.getStatusCode();
            if (isSuccess(statusCode)) {
                task.handleSuccess();
                return;
            }

            try {
                task.handleFailure(response.getStatusCode(), response.getResponseEntity());
            } catch (Throwable e) {
                log.error("Replication task {} error handler failure", task.getTaskName(), e);
            }
        }

        private ReplicationList createReplicationListOf(List<ReplicationTask> tasks) {
            ReplicationList list = new ReplicationList();
            for (ReplicationTask task : tasks) {
                // Only InstanceReplicationTask are batched.
                list.addReplicationInstance(createReplicationInstanceOf((InstanceReplicationTask) task));
            }
            return list;
        }

        private static boolean isSuccess(int statusCode) {
            return statusCode >= 200 && statusCode < 300;
        }

        /**
         * Check if the exception is some sort of network timeout exception (ie)
         * read,connect.
         *
         * @param e The exception for which the information needs to be found.
         * @return true, if it is a network timeout, false otherwise.
         */
        private static boolean isNetworkConnectException(Throwable e) {
            if (e instanceof IOException && !(e instanceof SSLException)) {
                return true;
            }

            var cause = e.getCause();
            if ((cause == null) || (cause == e)) {
                return false;
            }
            return isNetworkConnectException(cause);
        }

        /**
         * Check if the exception is socket read time out exception
         *
         * @param e The exception for which the information needs to be found.
         * @return true, if it may be a socket read time out exception.
         */
        private static boolean maybeReadTimeOut(Throwable e) {
            if (e instanceof IOException) {
                String message = e.getMessage();
                if (message != null) {
                    Matcher matcher = READ_TIME_OUT_PATTERN.matcher(message.toLowerCase());
                    if (matcher.find()) {
                        return true;
                    }
                }
            }

            var cause = e.getCause();
            if ((cause == null) || (cause == e)) {
                return false;
            }
            return maybeReadTimeOut(cause);
        }


        private static ReplicationInstance createReplicationInstanceOf(InstanceReplicationTask task) {
            ReplicationInstance.ReplicationInstanceBuilder instanceBuilder = aReplicationInstance();
            instanceBuilder.withAppName(task.getAppName());
            instanceBuilder.withId(task.getId());
            InstanceInfo instanceInfo = task.getInstanceInfo();
            if (instanceInfo != null) {
                String overriddenStatus = task.getOverriddenStatus() == null ? null : task.getOverriddenStatus().name();
                instanceBuilder.withOverriddenStatus(overriddenStatus);
                instanceBuilder.withLastDirtyTimestamp(instanceInfo.getLastDirtyTimestamp());
                if (task.shouldReplicateInstanceInfo()) {
                    instanceBuilder.withInstanceInfo(instanceInfo);
                }
                String instanceStatus = instanceInfo.getStatus() == null ? null : instanceInfo.getStatus().name();
                instanceBuilder.withStatus(instanceStatus);
            }
            instanceBuilder.withAction(task.getAction());
            return instanceBuilder.build();
        }
    }
}
