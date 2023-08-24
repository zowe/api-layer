/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import io.netty.handler.ssl.SslContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.zowe.apiml.cloudgatewayservice.service.WebClientHelper.load;

/**
 * Maintain all registered gateways lookup data. Internal caches uses apimlId is a key.
 * if apimlId key is not available then synthetic key is generated containing <code>SUBSTITUTE_</code> prefix and <code>instanceId</code>
 */
@Slf4j
@Service
public class GatewayIndexService {
    public static final String METADATA_APIML_ID_KEY = "apiml.service.apimlId";
    private final Cache<String, ServiceInstance> gatewayInstanceLookup;
    private final Cache<String, List<ServiceInfo>> gatewayServicesCache;
    private final WebClient defaultWebClient;
    private SslContext customClientSslContext = null;

    public GatewayIndexService(WebClient defaultWebClient,
                               @Value("${apiml.cloudGateway.cachePeriodSec:120}") int cachePeriodSec,
                               @Value("${apiml.cloudGateway.clientKeystore:#{null}}") String clientKeystorePath,
                               @Value("${apiml.cloudGateway.clientKeystorePassword:#{null}}") String clientKeystorePassword) {
        this.defaultWebClient = defaultWebClient;

        gatewayInstanceLookup = CacheBuilder.newBuilder().expireAfterWrite(cachePeriodSec, SECONDS).build();
        gatewayServicesCache = CacheBuilder.newBuilder().expireAfterWrite(cachePeriodSec, SECONDS).build();

        if (isNotBlank(clientKeystorePath) && isNotBlank(clientKeystorePassword)) {
            customClientSslContext = load(clientKeystorePath, clientKeystorePassword.toCharArray());
        }
    }

    @SneakyThrows
    private WebClient buildWebClient(ServiceInstance registration) {
        final String baseUrl = String.format("%s://%s:%d", registration.getScheme(), registration.getHost(), registration.getPort());
        if (this.customClientSslContext != null) {
            SslProvider sslProvider = SslProvider.builder().sslContext(customClientSslContext).build();
            HttpClient httpClient = HttpClient.create()
                    .secure(sslProvider);

            return WebClient.builder()
                    .baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .build();
        }

        return defaultWebClient.mutate()
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<List<ServiceInfo>> indexGatewayServices(ServiceInstance registration) {
        String apimlIdKey = extractApimlId(registration).orElse(buildAlternativeApimlIdKey(registration));
        log.debug("Fetching registered gateway instance services: {}", apimlIdKey);
        gatewayInstanceLookup.put(apimlIdKey, registration);
        return fetchServices(apimlIdKey, registration)
                .doOnError(ex -> log.warn("external GW call error", ex))
                .onErrorComplete()
                .doFinally(signal -> log.debug("\t {} completed with {}", apimlIdKey, signal));
    }

    private Mono<List<ServiceInfo>> fetchServices(String apimlId, ServiceInstance registration) {
        WebClient webClient = buildWebClient(registration);
        final ParameterizedTypeReference<List<ServiceInfo>> serviceInfoType = new ParameterizedTypeReference<List<ServiceInfo>>() {
        };

        return webClient.get().uri("/gateway/services")
                .retrieve()
                .bodyToMono(serviceInfoType)
                .doOnNext(foreignServices -> gatewayServicesCache.put(apimlId, foreignServices));
    }

    private String buildAlternativeApimlIdKey(ServiceInstance registration) {
        return "SUBSTITUTE" + "_" + registration.getInstanceId();
    }

    private Optional<String> extractApimlId(ServiceInstance registration) {
        if (registration.getMetadata() != null) {
            return Optional.ofNullable(registration.getMetadata().get(METADATA_APIML_ID_KEY));
        }
        return Optional.empty();
    }

    /**
     * list currently cached apiml registry with option to filter by the <code>apimlId</code> and <code>apiId</code>
     *
     * @param apimlId - filter for only services from the particular apiml instance, NULL - filter not applied
     * @param apiId   - filter for only services of particular type e.g. <code>bcm.sysview</code>
     * @return full of filter immutable map of the registry
     */
    public Map<String, List<ServiceInfo>> listRegistry(String apimlId, String apiId) {

        Map<String, List<ServiceInfo>> allServices = ImmutableMap.<String, List<ServiceInfo>>builder()
                .putAll(gatewayServicesCache.asMap()).build();
        return allServices.entrySet().stream()
                .filter(entry -> apimlId == null || StringUtils.equals(apimlId, entry.getKey()))
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), filterServicesByApiId(entry.getValue(), apiId)))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    List<ServiceInfo> filterServicesByApiId(List<ServiceInfo> apimlIdServices, String apiId) {
        if (!CollectionUtils.isEmpty(apimlIdServices)) {
            return apimlIdServices.stream()
                    .filter(serviceInfo -> apiId == null || hasSameApiId(serviceInfo, apiId))
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
}
