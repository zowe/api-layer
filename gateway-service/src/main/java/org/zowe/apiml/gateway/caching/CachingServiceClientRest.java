/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.caching;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.product.gateway.GatewayClient;
import reactor.core.publisher.Mono;

import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

@Component
@Slf4j
@ConditionalOnMissingBean(name = "modulithConfig")
public class CachingServiceClientRest implements CachingServiceClient {

    private static final String CACHING_SERVICE_RETURNED = ". Caching service returned: ";

    @Value("${apiml.cachingServiceClient.apiPath:/cachingservice/api/v1/cache}")
    private String CACHING_API_PATH;

    private volatile String cachingBalancerUrl;
    private final GatewayClient gatewayClient;

    private static final MultiValueMap<String, String> defaultHeaders = new LinkedMultiValueMap<>();

    static {
        defaultHeaders.add("Content-Type", "application/json");
    }

    private final WebClient webClient;

    public CachingServiceClientRest(
        @Qualifier("webClientClientCert") WebClient webClientClientCert,
        GatewayClient gatewayClient
    ) {
        this.gatewayClient = gatewayClient;
        this.webClient = webClientClientCert;
    }

    @PostConstruct
    void updateUrl() {
        // Lazy initialization of GatewayClient's ServerAddress may bring invalid URL during initialization
        this.cachingBalancerUrl = String.format("%s://%s/%s", gatewayClient.getGatewayConfigProperties().getScheme(), gatewayClient.getGatewayConfigProperties().getHostname(), CACHING_API_PATH);
    }


    public Mono<Void> create(ApiKeyValue keyValue) {
        updateUrl();
        Mono<Void> post = webClient.post()
            .uri(cachingBalancerUrl)
            .bodyValue(keyValue)
            .headers(c -> c.addAll(defaultHeaders))
            .exchangeToMono(handler -> {
                if (handler.statusCode().is2xxSuccessful()) {
                    return empty();
                } else {
                    log.debug("Unable to create cache record with result: {}", handler.statusCode());
                    return error(new CachingServiceClientException(handler.statusCode().value(), "Unable to create caching key " + keyValue.getKey() + CACHING_SERVICE_RETURNED + handler.statusCode()));
                }
            });
        post.doOnError(e -> log.debug("Unable to create cache record", e));

        return post;
    }

    public Mono<Void> update(ApiKeyValue keyValue) {
        updateUrl();
        Mono<Void> put = webClient.put()
            .uri(cachingBalancerUrl)
            .bodyValue(keyValue)
            .headers(c -> c.addAll(defaultHeaders))
            .exchangeToMono(handler -> {
                if (handler.statusCode().is2xxSuccessful()) {
                    return empty();
                } else {
                    return error(new CachingServiceClientException(handler.statusCode().value(), "Unable to update caching key " + keyValue.getKey() + CACHING_SERVICE_RETURNED + handler.statusCode()));
                }
            });
        put.doOnError(e -> log.debug("Unable to update cache key", e));

        return put;
    }

    public Mono<ApiKeyValue> read(String key) {
        updateUrl();
        Mono<ApiKeyValue> read = webClient.get()
            .uri(cachingBalancerUrl + "/" + key)
            .headers(c -> c.addAll(defaultHeaders))
            .exchangeToMono(handler -> {
                if (handler.statusCode().is2xxSuccessful()) {
                    return handler.bodyToMono(ApiKeyValue.class);
                } else if (handler.statusCode().is4xxClientError()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Key with ID {}not found. Status code from caching service: {}", key, handler.statusCode());
                    }
                    return empty();
                } else {
                    return error(new CachingServiceClientException(handler.statusCode().value(), "Unable to read caching key " + key + CACHING_SERVICE_RETURNED + handler.statusCode()));
                }
            });
        read.doOnError(e -> log.debug("Unable to read cache key", e));

        return read;
    }

    /**
     * Deletes {@link ApiKeyValue} from Caching Service
     *
     * @param key Key to delete
     * @return mono with status success / error
     */
    public Mono<Void> delete(String key) {
        updateUrl();
        Mono<Void> delete = webClient.delete()
            .uri(cachingBalancerUrl + "/" + key)
            .headers(c -> c.addAll(defaultHeaders))
            .exchangeToMono(handler -> {
                if (handler.statusCode().is2xxSuccessful()) {
                    return empty();
                } else {
                    return error(new CachingServiceClientException(handler.statusCode().value(), "Unable to delete caching key " + key + CACHING_SERVICE_RETURNED + handler.statusCode()));
                }
            });
        delete.doOnError(e -> log.debug("Unable to delete cache key", e));

        return delete;
    }

}
