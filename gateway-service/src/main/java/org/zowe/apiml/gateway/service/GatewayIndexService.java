/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.services.ServiceInfo;
import reactor.core.publisher.Mono;

import jakarta.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;

/**
 * Maintain all registered gateways lookup data. Internal caches uses apimlId is a key.
 * if apimlId key is not available then synthetic key is generated containing <code>SUBSTITUTE_</code> prefix and <code>instanceId</code>
 */
@Slf4j
@Service
public class GatewayIndexService {
    private final ApimlLogger apimlLog = ApimlLogger.of(GatewayIndexService.class, YamlMessageServiceInstance.getInstance());
    private final Cache<String, ServiceInstance> apimlGatewayLookup;
    private final Cache<String, List<ServiceInfo>> apimlServicesCache;
    private final WebClient webClient;

    public GatewayIndexService(
        @Qualifier("webClientClientCert") WebClient webClient,
        @Value("${apiml.cloudGateway.cachePeriodSec:120}") int cachePeriodSec
    ) {
        this.webClient = webClient;

        apimlGatewayLookup = CacheBuilder.newBuilder().expireAfterWrite(cachePeriodSec, SECONDS).build();
        apimlServicesCache = CacheBuilder.newBuilder().expireAfterWrite(cachePeriodSec, SECONDS).build();
    }

    private WebClient buildWebClient(ServiceInstance registration) {
        final String baseUrl = String.format("%s://%s:%d", registration.getScheme(), registration.getHost(), registration.getPort());

        return webClient.mutate()
            .baseUrl(baseUrl)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public Mono<List<ServiceInfo>> indexGatewayServices(ServiceInstance registration) {
        String apimlIdKey = extractApimlId(registration).orElse(buildAlternativeApimlIdKey(registration));
        log.debug("Fetching registered gateway instance services: {}", apimlIdKey);
        apimlGatewayLookup.put(apimlIdKey, registration);
        return fetchServices(apimlIdKey, registration)
            .doOnError(ex -> apimlLog.log("org.zowe.apiml.gateway.servicesRequestFailed", apimlIdKey, ex.getMessage()))
            .onErrorComplete()
            .doFinally(signal -> log.debug("\t {} completed with {}", apimlIdKey, signal));
    }

    /**
     * Store entry in the Services Registry. Should be used to store services info from the current apiml instance
     *
     * @param apimlId  unique apimlId
     * @param services List of the services
     */
    public void putApimlServices(@NotNull String apimlId, List<ServiceInfo> services) {
        apimlServicesCache.put(apimlId, services);
    }

    private Mono<List<ServiceInfo>> fetchServices(String apimlId, ServiceInstance registration) {
        final ParameterizedTypeReference<List<ServiceInfo>> serviceInfoType = new ParameterizedTypeReference<List<ServiceInfo>>() {
        };

        return buildWebClient(registration).get().uri("/gateway/services")
            .retrieve()
            .bodyToMono(serviceInfoType)
            .doOnNext(foreignServices -> apimlServicesCache.put(apimlId, foreignServices));
    }

    private String buildAlternativeApimlIdKey(ServiceInstance registration) {
        return "SUBSTITUTE" + "_" + registration.getInstanceId();
    }

    private Optional<String> extractApimlId(ServiceInstance registration) {
        return Optional.ofNullable(registration.getMetadata()).map(m -> m.get(APIML_ID));
    }

    /**
     * list currently cached apiml registry with option to filter by the <code>apimlId</code> and <code>apiId</code>
     *
     * @param apimlId   - filter for only services from the particular apiml instance, NULL - filter not applied
     * @param apiId     - filter for only services of particular type e.g. <code>zowe.apiml.apicatalog</code>
     * @param serviceId - filter for only services of the same serviceId e.g. <code>gateway</code>
     * @return full of filter immutable map of the registry
     */
    public Map<String, List<ServiceInfo>> listRegistry(String apimlId, String apiId, String serviceId) {

        Map<String, List<ServiceInfo>> allServices = ImmutableMap.<String, List<ServiceInfo>>builder()
            .putAll(apimlServicesCache.asMap()).build();
        return allServices.entrySet().stream()
            .filter(entry -> apimlId == null || StringUtils.equals(apimlId, entry.getKey()))
            .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), filterServicesByApiIdAndServiceId(entry.getValue(), apiId, serviceId)))
            .filter(entry -> !CollectionUtils.isEmpty(entry.getValue()))
            .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    List<ServiceInfo> filterServicesByApiIdAndServiceId(List<ServiceInfo> apimlIdServices, String apiId, String serviceId) {
        if (!CollectionUtils.isEmpty(apimlIdServices)) {
            return apimlIdServices.stream()
                .filter(Objects::nonNull)
                .filter(serviceInfo -> apiId == null || hasSameApiId(serviceInfo, apiId))
                .filter(serviceInfo -> serviceId == null || hasSameServiceId(serviceInfo, serviceId))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private boolean hasSameApiId(ServiceInfo serviceInfo, String apiId) {
        if (serviceInfo.getApiml() != null && !isEmpty(serviceInfo.getApiml().getApiInfo())) {
            return StringUtils.equals(apiId, serviceInfo.getApiml().getApiInfo().get(0).getApiId());
        }
        return false;
    }

    private boolean hasSameServiceId(ServiceInfo serviceInfo, String serviceId) {
        return StringUtils.equals(serviceId, serviceInfo.getServiceId());
    }
}
