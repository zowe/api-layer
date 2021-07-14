/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.cache;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

@RequiredArgsConstructor
public class CachingServiceClient {

    private final RestTemplate restTemplate;

    public void create(KeyValue kv) throws CachingServiceClientException {
        try {
            ResponseEntity<String> response = restTemplate.exchange("https://localhost:10010/cachingservice/api/v1/cache", HttpMethod.POST, new HttpEntity<KeyValue>(kv, new HttpHeaders()), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to create keyValue: " + kv.toString() + ", caused by: " + e.getMessage(), e);
        }
    }

    public KeyValue read(String key) throws CachingServiceClientException {
        try {
            ResponseEntity<KeyValue> response = response = restTemplate.exchange("https://localhost:10010/cachingservice/api/v1/cache" + "/" + key, HttpMethod.GET, new HttpEntity<KeyValue>(null, new HttpHeaders()), KeyValue.class);
            if (response != null && response.hasBody()) {
                return response.getBody();
            } else {
                throw new CachingServiceClientException("Unable to read key: " + key + ", caused by response from caching service is null or has no body");
            }
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to read key: " + key + ", caused by: " + e.getMessage(), e);
        }
    }

    public void update(KeyValue kv) throws CachingServiceClientException {
        try {
            ResponseEntity<String> response = restTemplate.exchange("https://localhost:10010/cachingservice/api/v1/cache", HttpMethod.PUT, new HttpEntity<KeyValue>(kv, new HttpHeaders()), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to update keyValue: " + kv.toString() + ", caused by: " + e.getMessage(), e);
        }
    }

    public void delete(String key) throws CachingServiceClientException {
        try {
            ResponseEntity<String> response = restTemplate.exchange("https://localhost:10010/cachingservice/api/v1/cache" + "/" + key, HttpMethod.DELETE, new HttpEntity<KeyValue>(null, new HttpHeaders()), String.class);
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
    static class KeyValue {
        private final String key;
        private final String value;
        private String serviceId;
        private final String created;

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
            this.serviceId = "";
            this.created = currentTime();
        }

        private static String currentTime() {
            return String.valueOf(new Date().getTime());
        }

        @JsonCreator
        public KeyValue() {
            key = "";
            value = "";
            serviceId = "";
            created = currentTime();
        }
    }

    public class CachingServiceClientException extends Exception {

        public CachingServiceClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public CachingServiceClientException(String message) {
            super(message);
        }
    }
}
