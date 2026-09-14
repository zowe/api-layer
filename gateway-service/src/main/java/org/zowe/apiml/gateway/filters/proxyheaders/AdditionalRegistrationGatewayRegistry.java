/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.proxyheaders;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.registry.client.RegistryCache;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Registry for APIML gateways discovered through additional registrations.
 * <p>
 * This class maintains a cache of cloud (central) and standard APIML gateways obtained via additional
 * registration sources. The cache is updated from {@link #watch(RegistryClient)}, which subscribes to the
 * client belonging to an additional registration.
 * <p>
 * The primary purpose of this class is to retain the IP addresses of other APIML gateways, so they
 * can be used to evaluate trusted proxy headers.
 */

@Component
@Slf4j
public class AdditionalRegistrationGatewayRegistry {

    @Value("${apiml.forwardHeader.trustedProxiesCacheTimeout:5m}")
    Duration registryExpiration;

    @Getter
    AtomicReference<Set<String>> additionalGatewayIpAddressesReference = new AtomicReference<>(Collections.emptySet());
    Cache<String, List<String>> knownAdditionalGateways;

    @PostConstruct
    public void init() {
        knownAdditionalGateways = CacheBuilder.newBuilder().expireAfterWrite(registryExpiration.toMillis(), MILLISECONDS).build();
        log.debug("AdditionalRegistrationGatewayRegistry initialized");
    }

    /**
     * Follows an additional registration's view of the other API ML.
     * <p>
     * Where this used to hang a {@code CacheRefreshedEvent} listener off a Eureka client, it now registers a
     * listener with the registry client directly - the type filtering the old handler had to do is the
     * listener interface's job.
     */
    public void watch(RegistryClient additionalApimlRegistration) {
        additionalApimlRegistration.addListener(this::onRefresh);
        log.debug("AdditionalRegistrationGatewayRegistry refresh registered for an additional registration");
    }

    void onRefresh(RegistryCache cache) {
        Set<String> additionalGateways = cache.upInstances(CoreService.GATEWAY.getServiceId()).stream()
            .flatMap(this::processInstanceInfoForIpAddresses)
            .collect(Collectors.toSet());
        log.debug("Additional registrations gateway ip addresses resolved: {}", additionalGateways);
        additionalGatewayIpAddressesReference.set(additionalGateways);
    }

    private InetAddress[] getInetAddressesByName(String instanceId, String networkName) {
        try {
            return InetAddress.getAllByName(networkName);
        } catch (UnknownHostException e) {
            log.debug("Unknown host or address for instance {} by {}", instanceId, networkName, e);
            return new InetAddress[0];
        }
    }

    private Stream<String> processInstanceInfoForIpAddresses(ServiceInstance instance) {
        try {
            return knownAdditionalGateways.get(instance.instanceId(), () -> {
                    List<String> addresses = Stream.of(
                            getInetAddressesByName(instance.instanceId(), instance.hostName()),
                            getInetAddressesByName(instance.instanceId(), instance.ipAddr())
                        )
                        .filter(Objects::nonNull)
                        .flatMap(Stream::of)
                        .map(InetAddress::getHostAddress)
                        .distinct()
                        .collect(Collectors.toList());
                    log.debug("Additional registrations gateway ip addresses for instance {} resolved: {}", instance.instanceId(), addresses);
                    return addresses;
                }
            ).stream();
        } catch (ExecutionException e) {
            log.debug("Unable to update additional gateway registry for instance {}.", instance, e);
            return Stream.empty();
        }
    }
}

