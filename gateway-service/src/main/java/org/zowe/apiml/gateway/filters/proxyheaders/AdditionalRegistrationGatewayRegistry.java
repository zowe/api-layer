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
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaEvent;
import com.netflix.discovery.shared.Application;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.constants.CoreService;

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
 * registration sources. The cache is automatically updated when the
 * {@code registerAdditionalRegistrationsGatewayRegistryRefresh} handler is registered with the
 * {@link DiscoveryClient} associated with the additional registration.
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

    public void registerCacheRefreshEventListener(DiscoveryClient additionalApimlRegistration) {
        additionalApimlRegistration.registerEventListener(
            event -> cacheRefreshEventHandler(event, additionalApimlRegistration));
        log.debug("AdditionalRegistrationGatewayRegistry refresh registered for additional registration: {}",
            additionalApimlRegistration.getEurekaClientConfig().getEurekaServerServiceUrls(null));
    }

    void cacheRefreshEventHandler(EurekaEvent event, DiscoveryClient additionalApimlRegistration) {
        if (event instanceof CacheRefreshedEvent) {
            Set<String> additionalGateways = Stream.of(
                    additionalApimlRegistration.getApplication(CoreService.GATEWAY.getServiceId())
                )
                .filter(Objects::nonNull)
                .map(Application::getInstances)
                .flatMap(List::stream)
                .flatMap(this::processInstanceInfoForIpAddresses)
                .collect(Collectors.toSet());
            log.debug("Additional registrations gateway ip addresses resolved: {}", additionalGateways);
            additionalGatewayIpAddressesReference.set(additionalGateways);
        }
    }

    private InetAddress[] getInetAddressesByName(String instanceId, String networkName) {
        try {
            return InetAddress.getAllByName(networkName);
        } catch (UnknownHostException e) {
            log.debug("Unknown host or address for instance {} by {}", instanceId, networkName, e);
            return new InetAddress[0];
        }
    }

    private Stream<String> processInstanceInfoForIpAddresses(InstanceInfo instanceInfo) {
        try {
            return knownAdditionalGateways.get(instanceInfo.getInstanceId(), () -> {
                    List<String> addresses = Stream.of(
                            getInetAddressesByName(instanceInfo.getInstanceId(), instanceInfo.getHostName()),
                            getInetAddressesByName(instanceInfo.getInstanceId(), instanceInfo.getIPAddr())
                        )
                        .filter(Objects::nonNull)
                        .flatMap(Stream::of)
                        .map(InetAddress::getHostAddress)
                        .distinct()
                        .collect(Collectors.toList());
                    log.debug("Additional registrations gateway ip addresses for instance {} resolved: {}", instanceInfo.getInstanceId(), addresses);
                    return addresses;
                }
            ).stream();
        } catch (ExecutionException e) {
            log.debug("Unable to update additional gateway registry for instance {}.", instanceInfo, e);
            return Stream.empty();
        }
    }
}

