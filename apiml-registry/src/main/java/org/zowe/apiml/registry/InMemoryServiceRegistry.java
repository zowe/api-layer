/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry;

import org.zowe.apiml.registry.internal.MeasuredRate;
import org.zowe.apiml.registry.model.ActionType;
import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.registry.policy.SelfPreservation;
import org.zowe.apiml.registry.policy.StatusOverridePolicy;
import org.zowe.apiml.registry.spi.RegistrationInterceptor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * In-memory service registry.
 * <p>
 * Everything the Eureka-based implementation achieved with reflection, subclass overrides and a ThreadLocal
 * counter correction is a first-class property here. Specifically:
 * <ul>
 *     <li>Static registrations get a {@link Lease.Kind#PERMANENT} lease instead of an anonymous Lease subclass
 *         whose {@code isExpired()} was hardcoded to false.</li>
 *     <li>Static registrations are excluded from the renew-threshold count by
 *         {@link RegistrationKind#countsTowardsRenewThreshold()}, replacing the {@code RENEW_CORRECTION}
 *         ThreadLocal that nudged Eureka's private counter on the way past.</li>
 *     <li>APIML's own instances are counted honestly rather than by adding a constant 2 to the renewal count.</li>
 *     <li>The delta queue, response invalidation and status overrides are ordinary fields rather than private
 *         Eureka state reached through {@code Field.setAccessible(true)}.</li>
 * </ul>
 * Thread safety: the instance map is concurrent and the mutating operations synchronise on {@link #lock} where
 * they touch more than one field, which is what keeps the renew-threshold accounting consistent with the map.
 */
public class InMemoryServiceRegistry implements ServiceRegistry {

    private static final long RENEWAL_WINDOW_MS = 60_000L;

    private final RegistrySettings config;
    private final StatusOverridePolicy statusPolicy;
    private final SelfPreservation selfPreservation;
    private final List<RegistrationInterceptor> interceptors;
    private final List<RegistryListener> listeners = new CopyOnWriteArrayList<>();
    private final LongSupplier clock;

    /** appName (upper case) -> instanceId -> entry. */
    private final Map<String, Map<String, Entry>> registry = new ConcurrentHashMap<>();

    /** Operator-set status overrides, keyed by instance id, surviving re-registration by design. */
    private final Map<String, InstanceStatus> statusOverrides = new ConcurrentHashMap<>();

    private final Deque<Change> recentChanges = new ArrayDeque<>();
    private final AtomicLong version = new AtomicLong(1);
    private final MeasuredRate renewals;

    private final Object lock = new Object();
    private volatile int expectedClientsSendingRenews;
    private volatile boolean openForTraffic;

    private record Entry(ServiceInstance instance, Lease lease, RegistrationKind kind) {
    }

    private record Change(long at, String appName, String instanceId) {
    }

    public InMemoryServiceRegistry(RegistrySettings config) {
        this(config, List.of(), System::currentTimeMillis);
    }

    public InMemoryServiceRegistry(
        RegistrySettings config,
        List<RegistrationInterceptor> interceptors,
        LongSupplier clock
    ) {
        this.config = config;
        this.statusPolicy = new StatusOverridePolicy();
        this.selfPreservation = new SelfPreservation(config);
        this.interceptors = interceptors.stream()
            .sorted(Comparator.comparingInt(RegistrationInterceptor::order))
            .toList();
        this.clock = clock;
        this.renewals = new MeasuredRate(RENEWAL_WINDOW_MS, clock.getAsLong());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Mutation
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public void register(ServiceInstance instance, RegistrationKind kind) {
        ServiceInstance incoming = instance;
        if (kind.subjectToInterceptors()) {
            for (RegistrationInterceptor interceptor : interceptors) {
                incoming = interceptor.intercept(incoming);
            }
        }

        long now = clock.getAsLong();
        String appName = key(incoming.appName());
        Map<String, Entry> instances = registry.computeIfAbsent(appName, name -> new ConcurrentHashMap<>());

        synchronized (lock) {
            Entry existing = instances.get(incoming.instanceId());

            // A brand-new dynamic or replicated instance raises the expected renewal rate. A static one must not,
            // or its silence would drag the observed rate below the threshold and suspend eviction for everyone.
            if (existing == null && kind.countsTowardsRenewThreshold() && expectedClientsSendingRenews > 0) {
                expectedClientsSendingRenews++;
            }

            // An override carried in on the registration seeds the map, but never displaces one already set - an
            // operator's decision outlives a service restart.
            InstanceStatus carried = incoming.overriddenStatus();
            if (carried != null && carried != InstanceStatus.UNKNOWN) {
                statusOverrides.putIfAbsent(incoming.instanceId(), carried);
            }
            InstanceStatus effectiveOverride = statusOverrides.get(incoming.instanceId());

            InstanceStatus resolved = statusPolicy.resolve(
                incoming,
                existing == null ? null : existing.instance(),
                kind.fromPeer(),
                statusOverrides
            );

            Lease lease = kind.permanentLease()
                ? Lease.permanent(now)
                : leaseFor(incoming, now, existing);

            ServiceInstance stored = incoming.toBuilder()
                .status(resolved)
                .overriddenStatus(effectiveOverride == null ? InstanceStatus.UNKNOWN : effectiveOverride)
                .lease(lease)
                .actionType(ActionType.ADDED)
                .lastUpdatedTimestamp(now)
                .build();

            instances.put(stored.instanceId(), new Entry(stored, lease, kind));
            recordChange(now, appName, stored.instanceId());
            publish(new RegistryEvent.InstanceRegistered(stored, kind, kind.fromPeer()));
        }
    }

    /**
     * Builds the lease for a dynamic registration, carrying over {@code serviceUpTimestamp} from any lease being
     * replaced so that "how long has this been up" survives a re-registration - matching Eureka.
     */
    private Lease leaseFor(ServiceInstance incoming, long now, Entry existing) {
        Lease requested = incoming.lease();
        int duration = requested == null ? config.defaultLeaseDurationSecs() : requested.durationSecs();
        int interval = requested == null ? config.defaultRenewalIntervalSecs() : requested.renewalIntervalSecs();
        long serviceUp = existing != null ? existing.lease().serviceUpTimestamp() : now;

        return Lease.builder()
            .kind(Lease.Kind.RENEWABLE)
            .durationSecs(duration <= 0 ? config.defaultLeaseDurationSecs() : duration)
            .renewalIntervalSecs(interval <= 0 ? config.defaultRenewalIntervalSecs() : interval)
            .registrationTimestamp(now)
            .lastRenewalTimestamp(now)
            .serviceUpTimestamp(serviceUp)
            .build();
    }

    @Override
    public boolean renew(String appName, String instanceId, boolean fromPeer) {
        long now = clock.getAsLong();
        Entry entry = lookup(appName, instanceId);
        if (entry == null) {
            return false;
        }

        Lease renewed = entry.lease().renewedAt(now);
        // A renewal is also the moment an override applied while the instance was away takes effect.
        InstanceStatus override = statusOverrides.get(instanceId);
        ServiceInstance updated = entry.instance().toBuilder()
            .lease(renewed)
            .overriddenStatus(override == null ? InstanceStatus.UNKNOWN : override)
            .build();

        registry.get(key(appName)).put(instanceId, new Entry(updated, renewed, entry.kind()));
        renewals.increment(now);
        publish(new RegistryEvent.InstanceRenewed(key(appName), instanceId, fromPeer));
        return true;
    }

    @Override
    public boolean cancel(String appName, String instanceId, boolean fromPeer) {
        return internalCancel(appName, instanceId, fromPeer, false);
    }

    private boolean internalCancel(String appName, String instanceId, boolean fromPeer, boolean expired) {
        long now = clock.getAsLong();
        String app = key(appName);
        Map<String, Entry> instances = registry.get(app);
        if (instances == null) {
            return false;
        }

        synchronized (lock) {
            Entry removed = instances.remove(instanceId);
            if (removed == null) {
                return false;
            }
            if (removed.kind().countsTowardsRenewThreshold() && expectedClientsSendingRenews > 0) {
                expectedClientsSendingRenews--;
            }
            // The override is intentionally retained: an operator's OUT_OF_SERVICE must still apply if the
            // instance comes back. Eureka behaves the same way.
            if (instances.isEmpty()) {
                registry.remove(app, instances);
            }
            recordChange(now, app, instanceId);
            publish(new RegistryEvent.InstanceCancelled(app, instanceId, fromPeer, expired));
            return true;
        }
    }

    @Override
    public boolean overrideStatus(String appName, String instanceId, InstanceStatus status, boolean fromPeer) {
        Entry entry = lookup(appName, instanceId);
        if (entry == null) {
            return false;
        }
        long now = clock.getAsLong();
        statusOverrides.put(instanceId, status);

        ServiceInstance updated = entry.instance().toBuilder()
            .overriddenStatus(status)
            .status(status)
            .lastUpdatedTimestamp(now)
            .actionType(ActionType.MODIFIED)
            .build();
        registry.get(key(appName)).put(instanceId, new Entry(updated, entry.lease(), entry.kind()));
        recordChange(now, key(appName), instanceId);
        publish(new RegistryEvent.StatusChanged(key(appName), instanceId, status, fromPeer));
        return true;
    }

    @Override
    public boolean clearStatusOverride(String appName, String instanceId, InstanceStatus revertTo, boolean fromPeer) {
        Entry entry = lookup(appName, instanceId);
        if (entry == null) {
            return false;
        }
        long now = clock.getAsLong();
        statusOverrides.remove(instanceId);

        InstanceStatus restored = revertTo == null ? InstanceStatus.UNKNOWN : revertTo;
        ServiceInstance updated = entry.instance().toBuilder()
            .overriddenStatus(InstanceStatus.UNKNOWN)
            .status(restored)
            .lastUpdatedTimestamp(now)
            .actionType(ActionType.MODIFIED)
            .build();
        registry.get(key(appName)).put(instanceId, new Entry(updated, entry.lease(), entry.kind()));
        recordChange(now, key(appName), instanceId);
        publish(new RegistryEvent.StatusChanged(key(appName), instanceId, restored, fromPeer));
        return true;
    }

    @Override
    public boolean updateMetadata(String appName, String instanceId, Map<String, String> metadata) {
        Entry entry = lookup(appName, instanceId);
        if (entry == null) {
            return false;
        }
        long now = clock.getAsLong();
        Map<String, String> merged = new TreeMap<>(entry.instance().metadata());
        merged.putAll(metadata);

        ServiceInstance updated = entry.instance().toBuilder()
            .metadata(merged)
            .lastUpdatedTimestamp(now)
            .actionType(ActionType.MODIFIED)
            .build();
        registry.get(key(appName)).put(instanceId, new Entry(updated, entry.lease(), entry.kind()));
        recordChange(now, key(appName), instanceId);
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Eviction
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean evictionAllowed() {
        return selfPreservation.evictionAllowed(expectedClientsSendingRenews, renewals.lastCount(clock.getAsLong()));
    }

    @Override
    public int evict(long additionalLeaseMs) {
        if (!evictionAllowed()) {
            return 0;
        }
        long now = clock.getAsLong();

        List<Entry> expired = new ArrayList<>();
        for (Map<String, Entry> instances : registry.values()) {
            for (Entry entry : instances.values()) {
                if (entry.lease().expired(now, additionalLeaseMs)) {
                    expired.add(entry);
                }
            }
        }
        if (expired.isEmpty()) {
            return 0;
        }

        int limit = selfPreservation.evictionLimit(size());
        int toEvict = Math.min(expired.size(), limit);
        if (toEvict <= 0) {
            return 0;
        }

        // Evict in random order. Iterating the map in order would empty one application entirely before touching
        // the next, so a partial eviction would take out a whole service rather than thinning several - this
        // spreads the damage while the eviction limit holds the rest back.
        Random random = new Random(now);
        int evicted = 0;
        for (int i = 0; i < toEvict; i++) {
            int next = i + random.nextInt(expired.size() - i);
            Entry chosen = expired.get(next);
            expired.set(next, expired.get(i));
            expired.set(i, chosen);
            if (internalCancel(chosen.instance().appName(), chosen.instance().instanceId(), false, true)) {
                evicted++;
            }
        }
        return evicted;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public Applications applications() {
        List<Application> apps = new ArrayList<>();
        // Sorted so that repeated reads of an unchanged registry produce identical bytes, which is what makes
        // apps__hashcode and client-side caching behave predictably.
        Map<String, Map<String, Entry>> sorted = new TreeMap<>(registry);
        for (Map.Entry<String, Map<String, Entry>> app : sorted.entrySet()) {
            List<ServiceInstance> instances = new ArrayList<>();
            new TreeMap<>(app.getValue()).values().forEach(entry -> instances.add(entry.instance()));
            if (!instances.isEmpty()) {
                apps.add(new Application(app.getKey(), instances));
            }
        }
        return new Applications(apps, version.get(), Applications.computeHashCode(apps));
    }

    @Override
    public Applications delta() {
        long now = clock.getAsLong();
        Map<String, List<ServiceInstance>> byApp = new LinkedHashMap<>();

        synchronized (recentChanges) {
            recentChanges.removeIf(change -> now - change.at() > config.deltaRetentionMs());
            for (Change change : recentChanges) {
                Entry entry = lookup(change.appName(), change.instanceId());
                ServiceInstance instance = entry != null
                    ? entry.instance()
                    // Gone from the registry, so the change was a removal: report it as DELETED so clients drop it
                    // rather than silently keeping a stale entry until their next full fetch.
                    : ServiceInstance.builder()
                        .instanceId(change.instanceId())
                        .appName(change.appName())
                        .actionType(ActionType.DELETED)
                        .status(InstanceStatus.DOWN)
                        .lastUpdatedTimestamp(change.at())
                        .build();
                byApp.computeIfAbsent(change.appName(), name -> new ArrayList<>()).add(instance);
            }
        }

        List<Application> apps = new ArrayList<>();
        byApp.forEach((name, instances) -> apps.add(new Application(name, instances)));
        // The hashcode describes the whole registry, not the delta - it is how a client checks whether applying
        // the delta left it consistent, so it must be computed from the full set.
        return new Applications(apps, version.get(), applications().appsHashCode());
    }

    @Override
    public Optional<Application> application(String appName) {
        Map<String, Entry> instances = registry.get(key(appName));
        if (instances == null || instances.isEmpty()) {
            return Optional.empty();
        }
        List<ServiceInstance> list = new ArrayList<>();
        new TreeMap<>(instances).values().forEach(entry -> list.add(entry.instance()));
        return Optional.of(new Application(key(appName), list));
    }

    @Override
    public Optional<ServiceInstance> instance(String appName, String instanceId) {
        return Optional.ofNullable(lookup(appName, instanceId)).map(Entry::instance);
    }

    @Override
    public List<ServiceInstance> byVipAddress(String vipAddress) {
        return matching(vipAddress, ServiceInstance::vipAddress);
    }

    @Override
    public List<ServiceInstance> bySecureVipAddress(String secureVipAddress) {
        return matching(secureVipAddress, ServiceInstance::secureVipAddress);
    }

    private List<ServiceInstance> matching(String address, java.util.function.Function<ServiceInstance, String> get) {
        if (address == null) {
            return List.of();
        }
        List<ServiceInstance> found = new ArrayList<>();
        for (Application application : applications().applications()) {
            for (ServiceInstance instance : application.instances()) {
                if (address.equalsIgnoreCase(get.apply(instance))) {
                    found.add(instance);
                }
            }
        }
        return found;
    }

    @Override
    public int size() {
        int total = 0;
        for (Map<String, Entry> instances : registry.values()) {
            total += instances.size();
        }
        return total;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public void openForTraffic(int expectedClients) {
        synchronized (lock) {
            this.expectedClientsSendingRenews = expectedClients;
            this.openForTraffic = true;
        }
        publish(new RegistryEvent.RegistryAvailable());
    }

    public boolean isOpenForTraffic() {
        return openForTraffic;
    }

    /** Exposed for the health endpoint and for the self-preservation tests. */
    public int expectedClientsSendingRenews() {
        return expectedClientsSendingRenews;
    }

    public long renewalsInLastMinute() {
        return renewals.lastCount(clock.getAsLong());
    }

    public int renewalThresholdPerMinute() {
        return selfPreservation.renewalThresholdPerMinute(expectedClientsSendingRenews);
    }

    @Override
    public void addListener(RegistryListener listener) {
        listeners.add(listener);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------------------------------------

    private Entry lookup(String appName, String instanceId) {
        Map<String, Entry> instances = registry.get(key(appName));
        return instances == null ? null : instances.get(instanceId);
    }

    /** appName is upper-cased on the wire by the Java enabler, so the registry keys on the upper-case form. */
    private static String key(String appName) {
        return appName == null ? "" : appName.toUpperCase();
    }

    private void recordChange(long now, String appName, String instanceId) {
        version.incrementAndGet();
        synchronized (recentChanges) {
            recentChanges.removeIf(change ->
                now - change.at() > config.deltaRetentionMs()
                    || (change.appName().equals(appName) && change.instanceId().equals(instanceId)));
            recentChanges.addLast(new Change(now, appName, instanceId));
        }
    }

    private void publish(RegistryEvent event) {
        for (RegistryListener listener : listeners) {
            listener.onRegistryEvent(event);
        }
    }

}
