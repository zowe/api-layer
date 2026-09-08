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

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zowe.apiml.discovery.staticdef.StaticServicesRegistrationService;
import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.model.DataCenterInfo;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.PortInfo;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Brings the registry up and keeps it swept.
 * <p>
 * Replaces three separate Eureka-era mechanisms: {@code EurekaRegistryAvailableListener} (which triggered static
 * registration), Eureka's own eviction timer, and the Discovery Service registering itself as a Eureka client of
 * itself. That last one is worth noting - the old arrangement had the Discovery Service run a
 * {@code EurekaClient} that talked over HTTP to its own server port in order to appear in its own registry, and
 * {@code EurekaConfig} had to call {@code eurekaClient.getApplications()} purely to force that client to
 * initialise. Here it simply inserts itself.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RegistryLifecycle implements ApplicationListener<ApplicationReadyEvent> {

    /*
     * Servlet only, i.e. the standalone Discovery Service.
     *
     * The modulith runs its own equivalent in ModulithConfig.onApplicationStart(), which registers the Gateway,
     * Discovery, Caching and Catalog instances itself. Letting this class run there as well registered a second,
     * competing `discovery` instance and re-ran static-definition loading - both nodes of the same registry
     * disagreeing about what is present. It is component-scanned from org.zowe.apiml, so without this condition
     * the modulith picks it up simply because it lives under that package.
     */

    private final ServiceRegistry registry;
    private final StaticServicesRegistrationService staticServicesRegistrationService;
    private final ServiceStartupEventHandler startupEventHandler;

    @Value("${apiml.service.id:discovery}")
    private String serviceId;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    @Value("${apiml.service.port:10011}")
    private int port;

    @Value("${server.ssl.enabled:true}")
    private boolean sslEnabled;

    @Value("${eureka.instance.ipAddress:#{null}}")
    private String configuredIpAddress;

    /**
     * How many clients we expect to be heartbeating once things settle.
     * <p>
     * Eureka called the equivalent {@code defaultOpenForTrafficCount} and defaulted it to 1. It seeds the
     * self-preservation threshold before any real client has registered; too high a value suspends eviction
     * indefinitely on a small deployment.
     */
    @Value("${eureka.server.defaultOpenForTrafficCount:1}")
    private int expectedClientsSendingRenews;

    @Value("${eureka.server.evictionIntervalTimerInMs:60000}")
    private long evictionIntervalMs;

    private final AtomicBoolean startUpInfoPublished = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        try {
            registerSelf();
        } catch (RuntimeException e) {
            // Never fatal. The registry's job is to serve other services; not appearing in its own registry is a
            // degradation, not a reason to refuse to start. Under Eureka this was implicitly the case because
            // self-registration went over HTTP asynchronously and its failures were only logged.
            log.error("The Discovery Service could not register itself; it will still serve other services", e);
        }

        // Opening for traffic publishes RegistryAvailable, which is what drives static registration - the same
        // ordering the Eureka-based implementation had, where static services were loaded from
        // EurekaRegistryAvailableEvent.
        registry.openForTraffic(expectedClientsSendingRenews);
        staticServicesRegistrationService.registerServices();

        if (startUpInfoPublished.compareAndSet(false, true)) {
            startupEventHandler.onServiceStartup("Discovery Service", ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
        }
    }

    /**
     * Puts the Discovery Service into its own registry.
     * <p>
     * Registered as SELF: never evicted, never counted towards the heartbeat-expected total - it does not
     * heartbeat to itself - and not run through the registration interceptors, because its own identity is not
     * third-party input. Under Eureka the renewal accounting was fudged instead, by
     * {@code ApimlInstanceRegistry.getNumOfRenewsInLastMin()} adding a constant 2 to the observed rate.
     */
    private void registerSelf() {
        String scheme = sslEnabled ? "https" : "http";
        String baseUrl = scheme + "://" + hostname + ":" + port;

        ServiceInstance self = ServiceInstance.builder()
            .instanceId(hostname + ":" + serviceId + ":" + port)
            .appName(serviceId.toUpperCase())
            .hostName(hostname)
            .ipAddr(configuredIpAddress)
            .port(new PortInfo(port, !sslEnabled))
            .securePort(new PortInfo(sslEnabled ? port : 0, sslEnabled))
            .vipAddress(serviceId)
            .secureVipAddress(serviceId)
            .status(InstanceStatus.UP)
            .homePageUrl(baseUrl + "/")
            .statusPageUrl(baseUrl + "/application/info")
            .secureHealthCheckUrl(sslEnabled ? baseUrl + "/application/health" : null)
            .healthCheckUrl(sslEnabled ? null : baseUrl + "/application/health")
            .dataCenterInfo(DataCenterInfo.MY_OWN)
            .lease(Lease.permanent(System.currentTimeMillis()))
            .build();

        registry.register(self, RegistrationKind.SELF);
        log.debug("Discovery Service registered itself as {}", self.instanceId());
    }

    /**
     * Expires lapsed leases.
     * <p>
     * The slack passed to {@code evict} compensates for this task not firing exactly on schedule - a long GC
     * pause or a busy scheduler would otherwise make leases look older than they are and evict healthy services.
     */
    @Scheduled(fixedDelayString = "${eureka.server.evictionIntervalTimerInMs:60000}")
    public void evictExpiredLeases() {
        if (!registry.evictionAllowed()) {
            log.debug("Lease expiration is currently disabled by self-preservation");
            return;
        }
        int evicted = registry.evict(0);
        if (evicted > 0) {
            log.info("Evicted {} expired instance(s)", evicted);
        }
    }

}
