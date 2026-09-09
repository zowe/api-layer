/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client;

import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A service's client for the registry: keeps a cached view current, and keeps its own registration alive.
 * <p>
 * Replaces {@code com.netflix.discovery.DiscoveryClient} for APIML's own services. It is deliberately passive -
 * it owns no threads. {@link #refresh()} and {@link #heartbeat()} are called by whatever scheduler the host
 * application already has, which keeps the class testable with an injected clock and stops it starting daemon
 * threads inside a Spring context that is about to be torn down. Eureka's client spawned several, and shutting
 * them down cleanly in tests was a recurring problem.
 */
public final class RegistryClient {

    private final RegistryTransport transport;
    private final RegistryCache cache = new RegistryCache();
    private final List<RegistryClientListener> listeners = new CopyOnWriteArrayList<>();

    /** This service's own registration, if it registers itself. */
    private final ServiceInstance self;

    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final AtomicLong consecutiveFetchFailures = new AtomicLong();
    private volatile boolean deltaSupported = true;

    public RegistryClient(RegistryTransport transport) {
        this(transport, null);
    }

    public RegistryClient(RegistryTransport transport, ServiceInstance self) {
        this.transport = transport;
        this.self = self;
    }

    public RegistryCache cache() {
        return cache;
    }

    public void addListener(RegistryClientListener listener) {
        listeners.add(listener);
    }

    public boolean registered() {
        return registered.get();
    }

    public long consecutiveFetchFailures() {
        return consecutiveFetchFailures.get();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Brings the cached view up to date.
     * <p>
     * Prefers a delta, falls back to a full fetch when the transport has none or when the delta leaves the view
     * inconsistent with the registry's declared hash code. That fallback is not defensive padding: applying a
     * delta to a stale base silently produces a routing table that disagrees with the registry, and the symptom
     * is traffic sent to an instance that no longer exists.
     *
     * @return true when the view changed
     */
    public boolean refresh() {
        try {
            if (deltaSupported) {
                Applications delta = transport.fetchDelta();
                if (delta == null) {
                    deltaSupported = false;
                } else if (cache.applyDelta(delta)) {
                    consecutiveFetchFailures.set(0);
                    notifyListeners();
                    return true;
                }
                // Delta left us inconsistent - fall through to a full fetch rather than serve a divergent view.
            }
            cache.replace(transport.fetchApplications());
            consecutiveFetchFailures.set(0);
            notifyListeners();
            return true;
        } catch (RegistryTransport.RegistryTransportException e) {
            // The last known view is kept on purpose. A registry that cannot be reached is not the same as a
            // registry that is empty, and discarding the cache here would stop the Gateway routing to services
            // that are perfectly healthy.
            consecutiveFetchFailures.incrementAndGet();
            return false;
        }
    }

    public Optional<ServiceInstance> instance(String appName, String instanceId) {
        return cache.application(appName)
            .flatMap(application -> application.instances().stream()
                .filter(candidate -> instanceId.equals(candidate.instanceId()))
                .findFirst());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Self-registration
    // ---------------------------------------------------------------------------------------------------------

    /** Registers this service. No-op when the client was built without a self instance. */
    public boolean register() {
        if (self == null) {
            return false;
        }
        try {
            transport.register(self);
            registered.set(true);
            return true;
        } catch (RegistryTransport.RegistryTransportException e) {
            registered.set(false);
            return false;
        }
    }

    /**
     * Renews this service's lease, re-registering if the registry has forgotten it.
     * <p>
     * The re-registration is the important part. A registry that restarts comes back empty, and a client that
     * only ever renews would keep receiving "not found" and never reappear - present in its own mind and absent
     * from every routing table.
     *
     * @return true when the lease is current afterwards
     */
    public boolean heartbeat() {
        if (self == null) {
            return false;
        }
        try {
            if (transport.renew(self.appName(), self.instanceId())) {
                registered.set(true);
                return true;
            }
            return register();
        } catch (RegistryTransport.RegistryTransportException e) {
            registered.set(false);
            return false;
        }
    }

    public void unregister() {
        if (self == null) {
            return;
        }
        try {
            transport.cancel(self.appName(), self.instanceId());
        } catch (RegistryTransport.RegistryTransportException e) {
            // Shutting down; a peer that never hears the cancel will expire the lease on its own.
        } finally {
            registered.set(false);
        }
    }

    private void notifyListeners() {
        for (RegistryClientListener listener : listeners) {
            listener.onRegistryRefreshed(cache);
        }
    }

}
