/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Drives a {@link RegistryClient}: registers this service, renews the lease, refreshes the cached view and
 * cancels the registration on shutdown.
 * <p>
 * The client itself owns no threads (see its javadoc); this is the one place a thread is started, and it is a
 * single daemon scheduler rather than the four pools Eureka's client created. It runs in the last startup
 * phase and the first shutdown phase, so the service is only advertised once it can serve requests and is
 * withdrawn before anything it depends on is torn down.
 */
@Slf4j
public class RegistryClientLifecycle implements SmartLifecycle {

    private final RegistryClient client;
    private final RegistryFetchProperties config;
    private final ApplicationEventPublisher publisher;

    /** Null when {@code eureka.client.healthcheck.enabled} is off, or actuator is not on the classpath. */
    private final HealthStatusSource healthStatusSource;

    private final AtomicReference<InstanceStatus> advertisedStatus = new AtomicReference<>();
    private volatile ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> heartbeatTask;
    private volatile ScheduledFuture<?> refreshTask;
    private volatile boolean running;

    public RegistryClientLifecycle(
        RegistryClient client,
        RegistryFetchProperties config,
        ApplicationEventPublisher publisher,
        HealthStatusSource healthStatusSource
    ) {
        this.client = client;
        this.config = config;
        this.publisher = publisher;
        this.healthStatusSource = healthStatusSource;
    }

    @Override
    public int getPhase() {
        // Last to start, first to stop.
        return Integer.MAX_VALUE;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "apiml-registry-client");
            thread.setDaemon(true);
            return thread;
        });

        if (config.isFetchRegistry()) {
            // Fetch once synchronously. A service whose first request arrives before the first scheduled
            // refresh would otherwise answer from an empty registry, which looks like an outage.
            client.refresh();
            publishRefreshed();
            refreshTask = scheduler.scheduleWithFixedDelay(
                this::refresh,
                config.getRegistryFetchIntervalSeconds(),
                config.getRegistryFetchIntervalSeconds(),
                TimeUnit.SECONDS);
        }

        if (config.isRegisterWithEureka()) {
            register();
            heartbeatTask = scheduler.scheduleWithFixedDelay(
                this::heartbeat,
                config.getInstanceInfoReplicationIntervalSeconds(),
                config.getInstanceInfoReplicationIntervalSeconds(),
                TimeUnit.SECONDS);
        }
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        cancel(heartbeatTask);
        cancel(refreshTask);
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (config.isRegisterWithEureka() && config.isShouldUnregisterOnShutdown()) {
            // Deliberately synchronous and before the web server closes: peers learn immediately instead of
            // waiting out a 90-second lease while requests are routed to a process that is already gone.
            client.unregister();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    // -------------------------------------------------------------------------------------------------------

    private void register() {
        if (!client.register()) {
            log.debug("Registration was not accepted yet; the next heartbeat will retry");
            return;
        }
        ServiceInstance self = client.self();
        publisher.publishEvent(new RegistryRegisteredEvent(this, self));
        advertisedStatus.set(self.status());
        promoteToHealthyStatus();
    }

    private void heartbeat() {
        try {
            boolean wasRegistered = client.registered();
            if (!client.heartbeat()) {
                log.debug("Lease renewal failed; will retry in {}s",
                    config.getInstanceInfoReplicationIntervalSeconds());
                return;
            }
            if (!wasRegistered) {
                publisher.publishEvent(new RegistryRegisteredEvent(this, client.self()));
            }
            promoteToHealthyStatus();
        } catch (RuntimeException e) {
            // A scheduled task that throws is never run again, which would silently stop the heartbeat and
            // let the registration expire.
            log.debug("Heartbeat failed", e);
        }
    }

    private void refresh() {
        try {
            if (client.refresh()) {
                publishRefreshed();
            } else {
                // The cached view is deliberately kept when the registry cannot be reached, so a failing fetch is
                // invisible from the outside: the service carries on routing from a stale table. Record it.
                log.debug("Registry fetch failed ({} in a row); still serving the last known view",
                    client.consecutiveFetchFailures());
            }
        } catch (RuntimeException e) {
            log.debug("Registry refresh failed", e);
        }
    }

    private void publishRefreshed() {
        publisher.publishEvent(new RegistryCacheRefreshedEvent(this, client.cache()));
    }

    /**
     * Moves the registered status to whatever the application's health says, and only when it changes.
     * <p>
     * The "only when it changes" is what keeps this cheap: an unconditional status update on every heartbeat
     * would make each renewal a write that peers have to replicate.
     */
    private void promoteToHealthyStatus() {
        InstanceStatus target = healthStatusSource == null
            ? InstanceStatus.UP
            : healthStatusSource.currentStatus();

        if (target != InstanceStatus.UP) {
            // A service that advertises itself unhealthy because of what it cannot see in the registry is
            // indistinguishable, from the outside, from one that is genuinely broken. Record its own view, and
            // how many fetches have failed in a row, so the two can be told apart.
            log.warn("Advertising {} after {} failed fetch(es); own registry view holds {} instance(s): {}",
                target, client.consecutiveFetchFailures(), client.cache().size(), describeCache());
        }

        if (target == InstanceStatus.UNKNOWN || target == advertisedStatus.get()) {
            return;
        }
        if (client.updateStatus(target)) {
            advertisedStatus.set(target);
            log.debug("Registered status is now {}", target);
        }
    }

    private String describeCache() {
        StringBuilder view = new StringBuilder();
        for (String serviceId : client.cache().serviceIds()) {
            if (view.length() > 0) {
                view.append(", ");
            }
            view.append(serviceId).append('=').append(client.cache().instances(serviceId).stream()
                .map(instance -> instance.instanceId() + ":" + instance.effectiveStatus())
                .collect(Collectors.joining("|")));
        }
        return view.toString();
    }

    private static void cancel(ScheduledFuture<?> task) {
        if (task != null) {
            task.cancel(true);
        }
    }

}
