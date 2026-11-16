/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.websocket;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Shared bounds include queued work and writes that have not yet returned. */
public class WebSocketRelayResources implements AutoCloseable {
    final ThreadPoolExecutor writers;
    final ThreadPoolExecutor connectors;
    private final ThreadPoolExecutor closers;
    private final ScheduledExecutorService clock;
    private final Set<PartialWebSocketRelay> connections = ConcurrentHashMap.newKeySet();
    private final Semaphore slots;
    private final long limit;
    private final AtomicLong pending = new AtomicLong();
    private boolean stopping;
    final AtomicInteger paused = new AtomicInteger();
    final MeterRegistry meters;
    final Timer writes;

    public WebSocketRelayResources(WebSocketProperties properties, MeterRegistry meters) {
        properties.validate();
        var settings = properties.getPartial();
        this.meters = meters;
        this.limit = settings.getGlobalPendingBytes();
        slots = new Semaphore(settings.getMaxConnections());
        writers = pool("websocket-writer-", settings.getWriterThreads(), 2 * settings.getMaxConnections());
        connectors = pool("websocket-connect-", settings.getConnectThreads(), settings.getMaxConnections());
        closers = pool("websocket-close-", 2, settings.getMaxConnections());
        clock = Executors.newSingleThreadScheduledExecutor(factory("websocket-timeouts-"));
        clock.scheduleWithFixedDelay(() -> connections.forEach(PartialWebSocketRelay::checkTimeouts), 100, 100, TimeUnit.MILLISECONDS);
        meters.gauge("apiml.websocket.pending.bytes", pending);
        meters.gauge("apiml.websocket.paused.reads", paused);
        meters.gauge("apiml.websocket.active", connections, Set::size);
        writes = meters.timer("apiml.websocket.write.duration");
    }

    private static ThreadFactory factory(String prefix) {
        var sequence = new AtomicInteger();
        return runnable -> {
            var thread = new Thread(runnable, prefix + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static ThreadPoolExecutor pool(String name, int threads, int queued) {
        return new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queued), factory(name), new ThreadPoolExecutor.AbortPolicy());
    }

    synchronized boolean register(PartialWebSocketRelay relay) {
        if (stopping || !slots.tryAcquire()) {
            failure("connections");
            return false;
        }
        connections.add(relay);
        meters.counter("apiml.websocket.connections", "mode", "partial").increment();
        return true;
    }

    boolean reserve(long bytes) {
        long current;
        do {
            current = pending.get();
            if (bytes > limit - current) {
                return false;
            }
        } while (!pending.compareAndSet(current, current + bytes));
        return true;
    }

    void release(long bytes) {
        pending.addAndGet(-bytes);
    }

    void failure(String reason) {
        meters.counter("apiml.websocket.failures", "reason", reason).increment();
    }

    void finish(PartialWebSocketRelay relay, Runnable cleanup) {
        // One cleanup task per admitted connection; its slot is retained until cleanup completes.
        closers.execute(() -> {
            try {
                cleanup.run();
            } finally {
                if (connections.remove(relay)) {
                    slots.release();
                }
                stopClosersIfDrained();
            }
        });
    }

    long pendingBytes() {
        return pending.get();
    }

    @Override
    public void close() {
        synchronized (this) {
            if (stopping) {
                return;
            }
            stopping = true;
        }
        clock.shutdownNow();
        connections.forEach(relay -> relay.fail("shutdown", "Gateway shutting down"));
        connectors.shutdownNow();
        writers.shutdown();
        stopClosersIfDrained();
        try {
            closers.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void stopClosersIfDrained() {
        // A concurrent onClose may have won termination but not yet submitted its cleanup.
        if (stopping && connections.isEmpty()) {
            closers.shutdown();
        }
    }
}
