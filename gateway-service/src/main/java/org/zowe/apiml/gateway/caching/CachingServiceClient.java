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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
public class CachingServiceClient {

    private static final String CACHING_SERVICE_RETURNED = ". Caching service returned: ";

    @Value("${apiml.cachingServiceClient.apiPath}")
    private static final String CACHING_API_PATH = "/cachingservice/api/v1/cache"; //NOSONAR parametrization provided by @Value annotation

    private final String cachingBalancerUrl;

    private static final MultiValueMap<String, String> defaultHeaders = new LinkedMultiValueMap<>();

    static {
        defaultHeaders.add("Content-Type", "application/json");
    }

    private final WebClient webClient;

    public CachingServiceClient(
        @Qualifier("webClientClientCert") WebClient webClientClientCert,
        GatewayClient gatewayClient
    ) {
        this.cachingBalancerUrl = String.format("%s://%s/%s", gatewayClient.getGatewayConfigProperties().getScheme(), gatewayClient.getGatewayConfigProperties().getHostname(), CACHING_API_PATH);
        this.webClient = webClientClientCert;
    }


    public Mono<Void> create(KeyValue keyValue) {
        return webClient.post()
            .uri(cachingBalancerUrl)
            .bodyValue(keyValue)
            .headers(c -> c.addAll(defaultHeaders))
            .exchangeToMono(handler -> {
                if (handler.statusCode().is2xxSuccessful()) {
                    return empty();
                } else {
                    return error(new CachingServiceClientException(handler.statusCode().value(), "Unable to create caching key " + keyValue.getKey() + CACHING_SERVICE_RETURNED + handler.statusCode()));
                }
            });
    }

    public Mono<Void> update(KeyValue keyValue) {
        return webClient.put()
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
    }

    public Mono<KeyValue> read(String key) {
        return webClient.get()
            .uri(cachingBalancerUrl + "/" + key)
            .headers(c -> c.addAll(defaultHeaders))
            .exchangeToMono(handler -> {
                if (handler.statusCode().is2xxSuccessful()) {
                    return handler.bodyToMono(KeyValue.class);
                } else if (handler.statusCode().is4xxClientError()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Key with ID " + key + "not found. Status code from caching service: " + handler.statusCode());
                    }
                    return empty();
                } else {
                    return error(new CachingServiceClientException(handler.statusCode().value(), "Unable to read caching key " + key + CACHING_SERVICE_RETURNED + handler.statusCode()));
                }
            });
    }

    /**
     * Deletes {@link KeyValue} from Caching Service
     *
     * @param key Key to delete
     * @return mono with status success / error
     */
    public Mono<Void> delete(String key) {
        return webClient.delete()
            .uri(cachingBalancerUrl + "/" + key)
            .headers(c -> c.addAll(defaultHeaders))
            .exchangeToMono(handler -> {
                if (handler.statusCode().is2xxSuccessful()) {
                    return empty();
                } else {
                    return error(new CachingServiceClientException(handler.statusCode().value(), "Unable to delete caching key " + key + CACHING_SERVICE_RETURNED + handler.statusCode()));
                }
            });
    }

    /**
     * Data POJO that represents entry in caching service
     */
    @RequiredArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Data
    public static class KeyValue {
        private final String key;
        private final String value;

        @JsonCreator
        public KeyValue() {
            key = "";
            value = "";
        }
    }

}
