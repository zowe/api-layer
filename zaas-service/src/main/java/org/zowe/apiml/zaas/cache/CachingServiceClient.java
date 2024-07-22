/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.cache;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;

import java.util.Map;


/**
 * Client for interaction with Caching Service
 * Supports basic CRUD operations
 */
@Slf4j
@SuppressWarnings({"squid:S1192"}) // literals are repeating in debug logs only
public class CachingServiceClient {

    private final GatewayClient gatewayClient;
    private final RestTemplate restTemplate;
    @Value("${apiml.cachingServiceClient.apiPath}")
    private static final String CACHING_API_PATH = "/cachingservice/api/v1/cache"; //NOSONAR parametrization provided by @Value annotation
    @Value("${apiml.cachingServiceClient.list.apiPath}")
    private static final String CACHING_LIST_API_PATH = "/cachingservice/api/v1/cache-list/"; //NOSONAR parametrization provided by @Value annotation

    private static final HttpHeaders defaultHeaders = new HttpHeaders();

    static {
        defaultHeaders.add("Content-Type", "application/json");
    }

    public static HttpHeaders getDefaultHeaders() {
        return defaultHeaders;
    }

    public CachingServiceClient(RestTemplate restTemplate, GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
        if (restTemplate == null) {
            throw new IllegalStateException("RestTemplate instance cannot be null");
        }
        this.restTemplate = restTemplate;
    }

    private String getGatewayAddress() {
        ServiceAddress gatewayAddress = gatewayClient.getGatewayConfigProperties();
        if (gatewayAddress.getScheme() == null || gatewayAddress.getHostname() == null) {
            throw new IllegalStateException("zaasProtocolHostPort has to have value in format <protocol>://<host>:<port> and not be null");
        }
        return String.format("%s://%s", gatewayAddress.getScheme(), gatewayAddress.getHostname());
    }

    /**
     * Creates {@link KeyValue} in Caching Service.
     *
     * @param kv {@link KeyValue} to store
     * @throws CachingServiceClientException when http response from caching is not 2xx, such as connect exception or cache conflict
     */

    public void create(KeyValue kv) throws CachingServiceClientException {
        try {
            restTemplate.exchange(getGatewayAddress() + CACHING_API_PATH, HttpMethod.POST, new HttpEntity<>(kv, defaultHeaders), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to create keyValue: " + kv.toString() + ", caused by: " + e.getMessage(), e);
        }
    }

    public void appendList(String mapKey, KeyValue kv) throws CachingServiceClientException {
        try {
            var url = getGatewayAddress() + CACHING_LIST_API_PATH + mapKey;
            log.debug("append list url: {}", url);
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(kv, defaultHeaders), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to create keyValue: " + kv.toString() + " in a map under " + mapKey + " key, caused by: " + e.getMessage(), e);
        }
    }

    public Map<String, Map<String, String>> readAllMaps() throws CachingServiceClientException {
        try {
            var responseType = new ParameterizedTypeReference<Map<String, Map<String, String>>>() {
            };
            var url = getGatewayAddress() + CACHING_LIST_API_PATH;
            log.debug("readAllMaps url: {}", url);
            var response = restTemplate.exchange(url, HttpMethod.GET, null, responseType);
            if (response.getStatusCode().is2xxSuccessful()) {
                if (response.getBody() != null && !response.getBody().isEmpty()) {     //NOSONAR tests return null
                    return response.getBody();
                }
                return null;
            } else {
                throw new CachingServiceClientException("Unable to read all key-value maps from cache list, caused by response from caching service is null or has no body");
            }
        } catch (Exception e) {
            throw new CachingServiceClientException("Unable to read all key-value maps from cache list, caused by: " + e.getMessage(), e);
        }
    }

    /**
     * Evict the non-relevant invalidated tokens by deleting the entries in the specified map
     *
     * @param key the map key
     */
    public void evictTokens(String key) {
        try {
            restTemplate.exchange(getGatewayAddress() + CACHING_LIST_API_PATH + "evict/tokens/" + key, HttpMethod.DELETE, new HttpEntity<>(null, defaultHeaders), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to delete key: " + key + ", caused by: " + e.getMessage(), e);
        }
    }

    /**
     * Evict the non-relevant rules by deleting the entries in the specified map
     *
     * @param key the map key
     */
    public void evictRules(String key) {
        try {
            restTemplate.exchange(getGatewayAddress() + CACHING_LIST_API_PATH + "evict/rules/" + key, HttpMethod.DELETE, new HttpEntity<>(null, defaultHeaders), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to delete key: " + key + ", caused by: " + e.getMessage(), e);
        }
    }


    /**
     * Reads {@link KeyValue} from Caching Service
     *
     * @param key Key to read
     * @return {@link KeyValue}
     * @throws CachingServiceClientException when http response from caching is not 2xx, such as connect exception or 404 key not found in cache
     */
    public KeyValue read(String key) throws CachingServiceClientException {
        try {
            ResponseEntity<KeyValue> response = restTemplate.exchange(getGatewayAddress() + CACHING_API_PATH + "/" + key, HttpMethod.GET, new HttpEntity<KeyValue>(null, defaultHeaders), KeyValue.class);
            if (response != null && response.hasBody()) { //NOSONAR tests return null
                return response.getBody();
            } else {
                throw new CachingServiceClientException("Unable to read key: " + key + ", caused by response from caching service is null or has no body");
            }
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to read key: " + key + ", caused by: " + e.getMessage(), e);
        }
    }

    /**
     * Updates {@link KeyValue} in Caching Service
     *
     * @param kv {@link KeyValue} to update
     * @throws CachingServiceClientException when http response from caching is not 2xx, such as connect exception or 404 key not found in cache
     */
    public void update(KeyValue kv) throws CachingServiceClientException {
        try {
            restTemplate.exchange(getGatewayAddress() + CACHING_API_PATH, HttpMethod.PUT, new HttpEntity<>(kv, defaultHeaders), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to update keyValue: " + kv.toString() + ", caused by: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes {@link KeyValue} from Caching Service
     *
     * @param key Key to delete
     * @throws CachingServiceClientException when http response from caching is not 2xx, such as connect exception or 404 key not found in cache
     */
    public void delete(String key) throws CachingServiceClientException {
        try {
            restTemplate.exchange(getGatewayAddress() + CACHING_API_PATH + "/" + key, HttpMethod.DELETE, new HttpEntity<KeyValue>(null, defaultHeaders), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to delete key: " + key + ", caused by: " + e.getMessage(), e);
        }
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
