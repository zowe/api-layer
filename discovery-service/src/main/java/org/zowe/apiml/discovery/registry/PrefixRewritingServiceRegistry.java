/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import lombok.RequiredArgsConstructor;
import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.RegistryListener;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Applies {@link ServiceIdPrefixRewriter} to every registry write.
 * <p>
 * A decorator rather than a registration interceptor, because the rewrite has to cover renew, cancel and status
 * update too - a service that registered under the old prefix goes on heartbeating under it, and those heartbeats
 * must reach the rewritten entry. Reads are deliberately <em>not</em> rewritten: storage already holds the new
 * names, so a read is asking about what is stored.
 * <p>
 * When the feature is unconfigured this delegates verbatim, and {@code RegistryConfiguration} does not even wrap
 * the registry, so the common case pays nothing.
 */
@RequiredArgsConstructor
public class PrefixRewritingServiceRegistry implements ServiceRegistry {

    private final ServiceRegistry delegate;
    private final ServiceIdPrefixRewriter rewriter;

    @Override
    public void register(ServiceInstance instance, RegistrationKind kind) {
        delegate.register(rewriter.rewrite(instance), kind);
    }

    @Override
    public boolean renew(String appName, String instanceId, boolean fromPeer) {
        String[] rewritten = rewriter.rewrite(appName, instanceId);
        return delegate.renew(rewritten[0], rewritten[1], fromPeer);
    }

    @Override
    public boolean cancel(String appName, String instanceId, boolean fromPeer) {
        String[] rewritten = rewriter.rewrite(appName, instanceId);
        return delegate.cancel(rewritten[0], rewritten[1], fromPeer);
    }

    @Override
    public boolean overrideStatus(String appName, String instanceId, InstanceStatus status, boolean fromPeer) {
        String[] rewritten = rewriter.rewrite(appName, instanceId);
        return delegate.overrideStatus(rewritten[0], rewritten[1], status, fromPeer);
    }

    @Override
    public boolean clearStatusOverride(String appName, String instanceId, InstanceStatus revertTo, boolean fromPeer) {
        String[] rewritten = rewriter.rewrite(appName, instanceId);
        return delegate.clearStatusOverride(rewritten[0], rewritten[1], revertTo, fromPeer);
    }

    @Override
    public boolean updateMetadata(String appName, String instanceId, Map<String, String> metadata) {
        String[] rewritten = rewriter.rewrite(appName, instanceId);
        return delegate.updateMetadata(rewritten[0], rewritten[1], metadata);
    }

    // Reads pass straight through - see the class comment.

    @Override
    public Applications applications() {
        return delegate.applications();
    }

    @Override
    public Applications delta() {
        return delegate.delta();
    }

    @Override
    public Optional<Application> application(String appName) {
        return delegate.application(appName);
    }

    @Override
    public Optional<ServiceInstance> instance(String appName, String instanceId) {
        return delegate.instance(appName, instanceId);
    }

    @Override
    public List<ServiceInstance> byVipAddress(String vipAddress) {
        return delegate.byVipAddress(vipAddress);
    }

    @Override
    public List<ServiceInstance> bySecureVipAddress(String secureVipAddress) {
        return delegate.bySecureVipAddress(secureVipAddress);
    }

    @Override
    public int evict(long additionalLeaseMs) {
        return delegate.evict(additionalLeaseMs);
    }

    @Override
    public boolean evictionAllowed() {
        return delegate.evictionAllowed();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public void addListener(RegistryListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void openForTraffic(int expectedClientsSendingRenews) {
        delegate.openForTraffic(expectedClientsSendingRenews);
    }

}
