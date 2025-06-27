package org.zowe.apiml.product.gateway;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaEvent;
import com.netflix.discovery.shared.Application;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.constants.CoreService;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

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
    }

    public void registerAdditionalRegistrationsGatewayRegistryRefresh(DiscoveryClient additionalApimlRegistration) {
        additionalApimlRegistration.registerEventListener(
            event -> cacheRefreshEventHandler(event, additionalApimlRegistration));
    }

    void cacheRefreshEventHandler(EurekaEvent event, DiscoveryClient additionalApimlRegistration) {
        if (event instanceof CacheRefreshedEvent) {
            Set<String> trustedProxies = Stream.of(
                    additionalApimlRegistration.getApplication(CoreService.GATEWAY.getServiceId()),
                    additionalApimlRegistration.getApplication(CoreService.CLOUD_GATEWAY.getServiceId())
                )
                .filter(Objects::nonNull)
                .map(Application::getInstances)
                .flatMap(List::stream)
                .flatMap(this::processInstanceInfoForIpAddresses)
                .collect(Collectors.toSet());
            log.debug("Additional registrations gateway ip addresses resolved: {}", trustedProxies);
            additionalGatewayIpAddressesReference.set(trustedProxies);
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
