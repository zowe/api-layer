package org.zowe.apiml.cloudgatewayservice.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import io.netty.handler.ssl.SslContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.zowe.apiml.cloudgatewayservice.service.WebClientHelper.load;

/**
 * Maintain external gateways lookup data. Internal caches uses apimlId is a key.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayIndexService {
    public static final String METADATA_APIML_ID_KEY = "apiml.service.apimlId";
    private static final int CACHE_PERIOD = 120;
    private final Cache<String, ServiceInstance> gatewayInstanceLookup = CacheBuilder.newBuilder().expireAfterWrite(CACHE_PERIOD, SECONDS).build();
    private final Cache<String, List<ServiceInfo>> alienGatewayServicesCache = CacheBuilder.newBuilder().expireAfterWrite(CACHE_PERIOD, SECONDS).build();
    private final Cache<String, String> domainInstances = CacheBuilder.newBuilder().expireAfterWrite(CACHE_PERIOD, SECONDS).build();
    private SslContext outboundSslContext = null;

    @PostConstruct
    public void init() {
        //TODO: come up with better WebClient Factory initialization
        try {
            this.outboundSslContext = load("/keys/ca32/random.p12", "secret".toCharArray());
        } catch (Exception ex) {
            log.warn("Outbound ssl context not created");
        }
    }

    @SneakyThrows
    private WebClient.Builder buildWebClientFactory(SslContext sslContext, ServiceInstance registration) {
        //todo response size 512Kb
        String baseUrl = String.format("%s://%s:%d", registration.getScheme(), registration.getHost(), registration.getPort());

        SslProvider sslProvider = SslProvider.builder().sslContext(sslContext).build();
        HttpClient httpClient = HttpClient.create()
                .secure(sslProvider);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/json");
    }

    public Mono<List<ServiceInfo>> indexGatewayServices(ServiceInstance registration) {
        String apimlId = extractApimlId(registration).orElse(null);
        log.debug("Fetching registered gateway instance services: {}", apimlId);
        final String domain = extractDomainName(registration);
        if (apimlId != null && outboundSslContext != null) {
            domainInstances.put(apimlId, domain);
            gatewayInstanceLookup.put(apimlId, registration);
            return fetchServices(apimlId, registration)
                    .doOnError(ex -> log.error("external GW call error", ex));
        }
        return Mono.empty();
    }

    private Mono<List<ServiceInfo>> fetchServices(String apimlId, ServiceInstance registration) {
        WebClient webClient = buildWebClientFactory(outboundSslContext, registration).build();
        final ParameterizedTypeReference<List<ServiceInfo>> serviceInfoType = new ParameterizedTypeReference<List<ServiceInfo>>() {
        };

        return webClient.get().uri("/gateway/services")
                .retrieve()
                .bodyToMono(serviceInfoType)
                .doOnNext(foreignServices -> alienGatewayServicesCache.put(apimlId, foreignServices));
    }

    private String extractDomainName(ServiceInstance registration) {
        //This is the general idea how to substitute tenantId
        String[] parts = StringUtils.splitByWholeSeparator(registration.getHost(), ".");
        int lastIndex = parts.length - 1;
        if (lastIndex > 0) {
            return parts[lastIndex - 1] + "." + parts[lastIndex];
        }
        return registration.getHost();
    }

    private Optional<String> extractApimlId(ServiceInstance registration) {
        if (registration.getMetadata() != null) {
            return Optional.ofNullable(registration.getMetadata().get(METADATA_APIML_ID_KEY));
        }
        return Optional.empty();
    }

    public void dumpIndex() {
        //todo - delete later
        log.info("Dump cache having {} records", domainInstances.size());
        for (Map.Entry<String, String> entry : domainInstances.asMap().entrySet()) {
            String apimlId = entry.getKey();
            List<ServiceInfo> remoteServices = alienGatewayServicesCache.getIfPresent(apimlId);
            if (remoteServices != null) {
                log.debug("\t {}-{} : found {} external services", entry.getValue(), apimlId, remoteServices.size());
                remoteServices.forEach(service -> log.trace("\t\t {}", service));
            }
        }
    }

    public Map<String, List<ServiceInfo>> getCurrentState() {
        return ImmutableMap.<String, List<ServiceInfo>>builder().putAll(alienGatewayServicesCache.asMap()).build();
    }
}
