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
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.zowe.apiml.cloudgatewayservice.service.WebClientHelper.load;

/**
 * Maintain external gateways lookup data. Internal caches uses apimlId is a key.
 */
@Slf4j
@Service
public class GatewayIndexService {
    public static final String METADATA_APIML_ID_KEY = "apiml.service.apimlId";
    private final Cache<String, ServiceInstance> gatewayInstanceLookup;
    private final Cache<String, List<ServiceInfo>> gatewayServicesCache;
    private final WebClient defaultWebClient;
    private SslContext customOutboundSslContext = null;

    public GatewayIndexService(WebClient defaultWebClient,
                               @Value("${apiml.cloudGateway.cachePeriodSec:120}") int cachePeriodSec,
                               @Value("${apiml.cloudGateway.outboundKeystore:#{null}}") String outboundKeystorePath,
                               @Value("${apiml.cloudGateway.outboundKeystorePassword:#{null}}") String outboundKeystorePassword) {
        this.defaultWebClient = defaultWebClient;

        gatewayInstanceLookup = CacheBuilder.newBuilder().expireAfterWrite(cachePeriodSec, SECONDS).build();
        gatewayServicesCache = CacheBuilder.newBuilder().expireAfterWrite(cachePeriodSec, SECONDS).build();

        if (isNotBlank(outboundKeystorePath) && isNotBlank(outboundKeystorePassword)) {
            customOutboundSslContext = load(outboundKeystorePath, outboundKeystorePassword.toCharArray());
        }
    }

    @SneakyThrows
    private WebClient.Builder buildWebClientFactory(ServiceInstance registration) {
        final String baseUrl = String.format("%s://%s:%d", registration.getScheme(), registration.getHost(), registration.getPort());
        if (this.customOutboundSslContext != null) {

            SslProvider sslProvider = SslProvider.builder().sslContext(customOutboundSslContext).build();
            HttpClient httpClient = HttpClient.create()
                    .secure(sslProvider);

            return WebClient.builder()
                    .baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .defaultHeader("Accept", "application/json");

        }
        return defaultWebClient.mutate()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json");
    }

    public Mono<List<ServiceInfo>> indexGatewayServices(ServiceInstance registration) {
        String apimlIdKey = extractApimlId(registration).orElse(buildAlternativeApimlIdKey(registration));
        log.debug("Fetching registered gateway instance services: {}", apimlIdKey);
        gatewayInstanceLookup.put(apimlIdKey, registration);
        return fetchServices(apimlIdKey, registration)
                .doOnError(ex -> log.error("external GW call error", ex))
                .doFinally(signal -> log.debug("\t {} completed with {}", apimlIdKey, signal));
    }

    private Mono<List<ServiceInfo>> fetchServices(String apimlId, ServiceInstance registration) {
        WebClient webClient = buildWebClientFactory(registration).build();
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

    public void dumpIndex() {
        //todo - delete later
        Set<String> apimlIds = gatewayServicesCache.asMap().keySet();
        log.info("Dump cache having {} records", apimlIds.size());
        for (String apimlId : apimlIds) {
            Map.Entry<String, List<ServiceInfo>> apimlServices = listRegistry(apimlId, null).entrySet().stream()
                    .findFirst().orElse(null);

            if (apimlServices != null) {
                log.debug("\t {}-{} : found {} external services", apimlId, apimlServices.getKey(), apimlServices.getValue().size());
                apimlServices.getValue().forEach(service -> log.trace("\t\t {}", service));
            }
        }
    }

    /*List<ServiceInfo> filterApimlIdServices(String apimlId, String apiId) {
        List<ServiceInfo> apimlIdServices = alienGatewayServicesCache.getIfPresent(apimlId);
        if (apimlIdServices != null) {
            return apimlIdServices.stream()
                    .filter(serviceInfo -> apiId==null || isSameApiId(serviceInfo, apiId))
                    .collect(Collectors.toList());

        }
        return Collections.emptyList();
    }*/

    public Map<String, List<ServiceInfo>> listRegistry(String apimlId, String apiId) {

        Map<String, List<ServiceInfo>> allServices = ImmutableMap.<String, List<ServiceInfo>>builder()
                .putAll(gatewayServicesCache.asMap()).build();
        return allServices.entrySet().stream()
                .filter(entry -> apimlId == null || StringUtils.equals(apimlId, entry.getKey()))
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().stream().filter(serviceInfo -> apiId == null || isSameApiId(serviceInfo, apiId)).collect(Collectors.toList())))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

    }

    private boolean isSameApiId(ServiceInfo serviceInfo, String apiId) {
        if (serviceInfo.getApiml() != null && !isEmpty(serviceInfo.getApiml().getApiInfo())) {
            return StringUtils.equals(apiId, serviceInfo.getApiml().getApiInfo().get(0).getApiId());
        }
        return false;
    }
}
