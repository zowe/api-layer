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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.zowe.apiml.discovery.registry.event.RegistryAvailableEvent;
import org.zowe.apiml.discovery.registry.event.RegistryInstanceRegisteredEvent;
import org.zowe.apiml.registry.RegistryEvent;
import org.zowe.apiml.registry.RegistryListener;
import org.zowe.apiml.registry.ServiceRegistry;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Republishes registry events as Spring application events.
 * <p>
 * This is the seam that keeps the registry core free of Spring while letting the rest of APIML - the Gateway's
 * route refresh, the Catalog's availability tracking, the health indicators - carry on listening the way they
 * always have.
 */
@Component
@RequiredArgsConstructor
public class SpringRegistryEventBridge implements RegistryListener {

    private final ServiceRegistry registry;
    private final ApplicationEventPublisher publisher;

    /** Passed as the heartbeat's value; consumers use it to tell one refresh from the next. */
    private final AtomicLong heartbeatCount = new AtomicLong();

    @PostConstruct
    void subscribe() {
        registry.addListener(this);
    }

    /*
     * if-else over instanceof rather than a pattern switch: switch patterns are preview on the Java 17 baseline.
     */
    @Override
    public void onRegistryEvent(RegistryEvent event) {
        if (event instanceof RegistryEvent.InstanceRegistered registered) {
            publisher.publishEvent(
                new RegistryInstanceRegisteredEvent(this, registered.instance(), registered.fromPeer()));
            publishHeartbeat();
        } else if (event instanceof RegistryEvent.RegistryAvailable) {
            publisher.publishEvent(new RegistryAvailableEvent(this));
            publishHeartbeat();
        }
        // Renew, cancel and status-change are deliberately not republished. Renewals arrive once per service
        // every thirty seconds; turning each into a Spring event would rebuild the Gateway route table
        // continuously for no benefit. Listeners that need those can subscribe to the registry directly.
    }

    /**
     * Publishes Spring Cloud's {@link HeartbeatEvent} whenever the registry's content changes.
     * <p>
     * This is not a courtesy event. Two components listen for it because Eureka's client emitted it on every
     * cache refresh, and the Discovery Service depended on it in its own right:
     * <ul>
     *     <li>{@code GatewayInstanceInitializer} re-resolves the Gateway's address. It also runs once on
     *     {@code ApplicationReadyEvent}, but the Gateway has not registered by then - the Discovery Service is
     *     ready first - so that attempt finds nothing. With no heartbeat to retry on it never looks again, and
     *     {@code GatewayClient} stays uninitialised for the life of the process. The Gateway is how
     *     {@code /application/**} authenticates on this service, through
     *     {@code GatewayLoginProvider} - so the {@code eurekaversion} endpoint the integration startup check
     *     reads on every instance answered 401 with {@code ZWEAS120E} and roughly thirty CI jobs died in
     *     {@code areDiscoveryInSync()} before they ran a single test.</li>
     *     <li>{@code RouteRefreshListener} rebuilds the Gateway's routes, in the modulith.</li>
     * </ul>
     * The 401 is worth spelling out, because the code in the body is misleading. A gateway login that cannot
     * even be attempted raises {@code GatewayNotAvailableException}, which
     * {@code GatewayLoginProvider} converts to {@code AuthenticationServiceException}. Spring Security's
     * {@code ProviderManager} treats any {@code AuthenticationException} as "this provider declined, try the
     * next one", so it falls through to the Discovery Service's own
     * {@code EurekaBasicAuthenticationProvider} - configured with {@code apiml.discovery.userid}, not with the
     * credentials the caller sent - and it is <em>that</em> failure which surfaces. The client is told its
     * username and password are wrong when in fact nothing was ever asked.
     * <p>
     * Published on a registration because that is when the answer to "is the Gateway there yet?" can change,
     * and on opening for traffic because that is when static registrations land. Both are rare - a service
     * registers once - so this is nothing like the 30-second cadence Eureka's client produced. It is published
     * on replicated registrations too: a peer bringing the Gateway in is exactly the case that was broken.
     * <p>
     * The Discovery Service has no {@code RegistryCacheRefreshedEvent} to publish alongside it: that type lives
     * in {@code apiml-registry-client-spring}, and this service deliberately does not depend on that module -
     * two {@code eurekaversion} endpoints in one context stop the application from starting, which is why that
     * module backs its own off for the services that serve the endpoint themselves.
     */
    private void publishHeartbeat() {
        publisher.publishEvent(new HeartbeatEvent(this, heartbeatCount.incrementAndGet()));
    }

}
