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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

@Component
@RequiredArgsConstructor
public class CachingServiceClient {

    @Value("${apiml.cachingServiceClient.apiPath}")
    private static final String CACHING_API_PATH = "/cachingservice/api/v1/cache";
    @Value("${apiml.cachingServiceClient.list.apiPath}")
    private static final String CACHING_LIST_API_PATH = "/cachingservice/api/v1/cache-list/";

    public static final String LOAD_BALANCER_KEY_PREFIX = "lb.";

    private static final MultiValueMap<String, String> defaultHeaders = new LinkedMultiValueMap<>();
    static {
        defaultHeaders.add("Content-Type", "application/json");
    }

    private final WebClient webClient;
    private final String gatewayProtocolHostPort;

    public Mono<Void> create(KeyValue keyValue) {
        return webClient.post()
            .uri(gatewayProtocolHostPort)
            .bodyValue(keyValue)
            .headers(c -> c.addAll(defaultHeaders))
            .exchangeToMono(handler -> {
                if (handler.statusCode().is2xxSuccessful()) {
                    return empty();
                } else {
                    return error(new CachingServiceClientException("Unable to create caching key " + keyValue.getKey() + ". Caching service returned: " + handler.statusCode()));
                }
            });
    }

    public Mono<Void> update(KeyValue keyValue) {
        return webClient.put()
            .uri(gatewayProtocolHostPort)
            .bodyValue(keyValue)
            .headers(c -> c.addAll(defaultHeaders))
            .exchangeToMono(handler -> {
                if (handler.statusCode().is2xxSuccessful()) {
                    return empty();
                } else {
                    return error(new RuntimeException(""));
                }
            });
    }

    public Mono<KeyValue> read(String key) {
        return webClient.get()
            .uri(gatewayProtocolHostPort + CACHING_API_PATH + "/" + key)
            .headers(c -> c.addAll(defaultHeaders))
            .exchangeToMono(handler -> {
                if (handler.statusCode().is2xxSuccessful()) {
                    return handler.bodyToMono(KeyValue.class);
                } else {
                    return error(new CachingServiceClientException("key"));
                }
            });
    }

    public Mono<Void> delete(String key) {
        // gatewayProtocolHostPort + CACHING_API_PATH + "/" + key
        return webClient.delete()
            .uri(gatewayProtocolHostPort + CACHING_API_PATH + "/" + key)
            .headers(c -> c.addAll(defaultHeaders))
            .exchangeToMono(handler -> {
                if (handler.statusCode().is2xxSuccessful()) {
                    return empty();
                } else {
                    return error(new CachingServiceClientException(key));
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
