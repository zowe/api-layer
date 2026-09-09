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

import org.zowe.apiml.registry.model.ActionType;
import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A client's view of the registry.
 * <p>
 * Holds the last full fetch and folds deltas into it. Deltas are the reason clients can poll every thirty seconds
 * without shipping the whole registry each time, and the reason a client has to be able to detect that its view
 * has diverged - which is what {@code appsHashCode} is for: after applying a delta, the client recomputes the
 * hash from its own contents and compares. A mismatch means "give up and do a full fetch" rather than "carry on
 * with a subtly wrong routing table".
 */
public final class RegistryCache {

    private final AtomicReference<Applications> current =
        new AtomicReference<>(new Applications(List.of(), 0L, ""));

    public Applications applications() {
        return current.get();
    }

    /** Replaces the whole view, as after a full fetch. */
    public void replace(Applications applications) {
        current.set(applications);
    }

    /**
     * Folds a delta into the current view.
     *
     * @return true when the result matches the delta's declared hash code; false means the view has diverged and
     *         the caller should do a full fetch
     */
    public boolean applyDelta(Applications delta) {
        Applications base = current.get();
        Map<String, Map<String, ServiceInstance>> byApp = new TreeMap<>();
        for (Application application : base.applications()) {
            Map<String, ServiceInstance> instances = new LinkedHashMap<>();
            application.instances().forEach(instance -> instances.put(instance.instanceId(), instance));
            byApp.put(application.name(), instances);
        }

        for (Application application : delta.applications()) {
            for (ServiceInstance instance : application.instances()) {
                ActionType action = instance.actionType();
                Map<String, ServiceInstance> instances =
                    byApp.computeIfAbsent(application.name(), name -> new LinkedHashMap<>());
                if (action == ActionType.DELETED) {
                    instances.remove(instance.instanceId());
                } else {
                    // ADDED and MODIFIED both mean "this is the current state of that instance".
                    instances.put(instance.instanceId(), instance);
                }
            }
        }

        List<Application> merged = new ArrayList<>();
        byApp.forEach((name, instances) -> {
            if (!instances.isEmpty()) {
                merged.add(new Application(name, new ArrayList<>(instances.values())));
            }
        });

        Applications result = new Applications(merged, delta.version(), delta.appsHashCode());
        current.set(result);

        String expected = delta.appsHashCode();
        if (expected == null || expected.isEmpty()) {
            // Nothing to check against; treat as consistent rather than forcing a needless full fetch.
            return true;
        }
        return expected.equals(Applications.computeHashCode(merged));
    }

    public Optional<Application> application(String appName) {
        if (appName == null) {
            return Optional.empty();
        }
        return current.get().applications().stream()
            .filter(application -> application.name().equalsIgnoreCase(appName))
            .findFirst();
    }

    /** Routable instances of a service: UP, honouring any status override. */
    public List<ServiceInstance> upInstances(String appName) {
        return application(appName)
            .map(Application::instances)
            .orElseGet(List::of)
            .stream()
            .filter(instance -> instance.effectiveStatus() == InstanceStatus.UP)
            .toList();
    }

    public List<String> serviceIds() {
        return current.get().applications().stream()
            .map(Application::name)
            .map(String::toLowerCase)
            .distinct()
            .toList();
    }

    public int size() {
        return current.get().applications().stream().mapToInt(Application::size).sum();
    }

}
