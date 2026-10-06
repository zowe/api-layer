/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.api;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.cache.Storage;
import org.zowe.apiml.cache.StorageException;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.config.ApplicationInfo;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.filter.CategorizeCertsFilter;
import reactor.core.publisher.Mono;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/cachingservice/api/v1")
public class CachingController {
    private final Storage storage;
    private final MessageService messageService;

    @Value("${caching.storage.maxQueryKeys:#{T(org.zowe.apiml.cache.PATRevocationStore).DEFAULT_MAX_QUERY_KEYS}}")
    int maxQueryKeys;

    @Autowired(required = false)
    ApplicationInfo applicationInfo;


    @GetMapping(value = {"/cache", "/cache/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves all values in the cache",
        description = "Values returned for the calling service")
    @ResponseBody
    public Mono<ResponseEntity<Object>> getAllValues(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> new ResponseEntity<>(storage.readForService(requireServiceId(exchange)), HttpStatus.OK));
    }

    @DeleteMapping(value = {"/cache", "/cache/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete all values for service from the cache",
        description = "Will delete all key-value pairs for specific service")
    public Mono<ResponseEntity<Object>> deleteAllValues(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            storage.deleteForService(requireServiceId(exchange));
            return new ResponseEntity<>(HttpStatus.OK);
        });
    }

    @GetMapping(value = "/cache/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves a specific value in the cache",
        description = "Value returned is for the provided {key}")
    @ResponseBody
    public Mono<ResponseEntity<Object>> getValue(@PathVariable String key, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> new ResponseEntity<>(storage.read(requireServiceId(exchange), key), HttpStatus.OK));
    }

    @DeleteMapping(value = "/cache/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete key from the cache",
        description = "Will delete key-value pair for the provided {key}")
    public Mono<ResponseEntity<Object>> delete(@PathVariable String key, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> new ResponseEntity<>(storage.delete(requireServiceId(exchange), key), HttpStatus.NO_CONTENT));
    }

    @PostMapping(value = {"/cache", "/cache/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new key in the cache",
        description = "A new key-value pair will be added to the cache")
    public Mono<ResponseEntity<Object>> createKey(@RequestBody KeyValue keyValue, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            String serviceId = requireServiceId(exchange);
            checkForInvalidPayload(keyValue);
            storage.create(serviceId, keyValue);
            return new ResponseEntity<>(HttpStatus.CREATED);
        });
    }

    @PostMapping(value = "/cache-list/{mapKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a new item in the cache map",
        description = "A new key-value pair will be added to the specific cache map with given map key.")
    public Mono<ResponseEntity<Object>> storeMapItem(@PathVariable String mapKey, @RequestBody KeyValue keyValue, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            String serviceId = requireServiceId(exchange);
            log.debug("All map for serviceId: {}", serviceId);
            checkForInvalidPayload(keyValue);
            storage.storeMapItem(serviceId, mapKey, keyValue);
            return new ResponseEntity<>(HttpStatus.CREATED);
        });
    }

    @GetMapping(value = "/cache-list/{mapKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves all the items in the cache map",
        description = "Values returned for the calling service and specific cache map. Deprecated: this " +
            "scans every item of the map. Use /cache-query to look up specific items.",
        deprecated = true)
    public Mono<ResponseEntity<Object>> getAllMapItems(@PathVariable String mapKey, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            String serviceId = requireServiceId(exchange);
            log.debug("Storing for serviceId: {}", serviceId);
            return new ResponseEntity<>(storage.getAllMapItems(serviceId, mapKey), HttpStatus.OK);
        });
    }

    @GetMapping(value = {"/cache-list", "/cache-list/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves all the maps in the cache",
        description = "Values returned for the calling service. Deprecated: this scans every item of every " +
            "map, so its cost grows with the size of the store. Use /cache-query to look up specific items.",
        deprecated = true)
    public Mono<ResponseEntity<Object>> getAllMaps(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            String serviceId = requireServiceId(exchange);
            log.debug("Get all for serviceId: {}", serviceId);
            return new ResponseEntity<>(storage.getAllMaps(serviceId), HttpStatus.OK);
        });
    }

    @PostMapping(value = {"/cache-query", "/cache-query/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Looks up specific items across cache maps",
        description = "Takes the item keys to look up grouped by map key, and returns only the entries that " +
            "exist. A map with no matching item is omitted from the response.")
    public Mono<ResponseEntity<Object>> getMapItems(@RequestBody Map<String, List<String>> keysByMapKey, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            String serviceId = requireServiceId(exchange);
            Map<String, Collection<String>> request = checkQueryPayload(keysByMapKey);
            return new ResponseEntity<>(storage.getMapItems(serviceId, request), HttpStatus.OK);
        });
    }

    @GetMapping(value = {"/cache-list-legacy", "/cache-list-legacy/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves all the maps stored in the pre-cutover layout",
        description = "Only for personal access tokens issued before the per-item revocation store was " +
            "introduced. Removed once every such token has expired.",
        deprecated = true)
    public Mono<ResponseEntity<Object>> getAllLegacyMaps(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            String serviceId = requireServiceId(exchange);
            log.debug("Get all legacy maps for serviceId: {}", serviceId);
            return new ResponseEntity<>(storage.getAllLegacyMaps(serviceId), HttpStatus.OK);
        });
    }

    @DeleteMapping(value = "/cache-list/evict/rules/{mapKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a record from a rules map in the cache",
        description = "Will delete a key-value pair from a specific rules map")
    public Mono<ResponseEntity<Object>> evictRules(@PathVariable String mapKey, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            String serviceId = requireServiceId(exchange);
            log.debug("Delete record for serviceId: {}", serviceId);
            storage.removeNonRelevantRules(serviceId, mapKey);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        });
    }

    @DeleteMapping(value = "/cache-list/evict/tokens/{mapKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a record from an invalid tokens map in the cache",
        description = "Will delete a key-value pair from a specific tokens map")
    public Mono<ResponseEntity<Object>> evictTokens(@PathVariable String mapKey, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            String serviceId = requireServiceId(exchange);
            log.debug("Evict tokens for serviceId: {}", serviceId);
            storage.removeNonRelevantTokens(serviceId, mapKey);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        });
    }

    @PutMapping(value = {"/cache", "/cache/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update key in the cache",
        description = "Value at the key in the provided key-value pair will be updated to the provided value")
    public Mono<ResponseEntity<Object>> update(@RequestBody KeyValue keyValue, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            String serviceId = requireServiceId(exchange);
            checkForInvalidPayload(keyValue);
            storage.update(serviceId, keyValue);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        });
    }

    @ExceptionHandler(MissingCertificateException.class)
    public ResponseEntity<Object> handleMissingCertificate(MissingCertificateException exception) {
        log.debug("Rejecting the request", exception);
        Messages missingCert = Messages.MISSING_CERTIFICATE;
        Message message = messageService.createMessage(missingCert.getKey(), "parameter");
        return new ResponseEntity<>(message.mapToView(), missingCert.getStatus());
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Object> handleStorageException(StorageException exception) {
        log.debug("Storage exception", exception);
        Message message = messageService.createMessage(exception.getKey(), (Object[]) exception.getParameters());
        return new ResponseEntity<>(message.mapToView(), exception.getStatus());
    }

    /**
     * Errors Spring itself raises for a request it cannot process, such as an unreadable body, keep their
     * own status instead of falling through to {@link #handleInternalError}.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public void handleResponseStatusException(ResponseStatusException exception) {
        throw exception;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleInternalError(Exception exception, ServerWebExchange exchange) {
        log.debug("Internal error occurred", exception);
        Messages internalServerError = Messages.INTERNAL_SERVER_ERROR;
        Message message = messageService.createMessage(internalServerError.getKey(), exchange.getRequest().getURI().toString(), exception.getMessage(), exception.toString());
        return new ResponseEntity<>(message.mapToView(), internalServerError.getStatus());
    }

    private String requireServiceId(ServerWebExchange exchange) {
        return getServiceId(exchange).orElseThrow(MissingCertificateException::new);
    }

    private Optional<String> getServiceId(ServerWebExchange exchange) {
        Optional<String> certificateServiceId = extractFromSslInfo(exchange);
        if (certificateServiceId.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> specificServiceId = getHeader(exchange, "X-CS-Service-ID");

        return specificServiceId.map(s -> certificateServiceId.get() + ", SERVICE=" + s).or(() -> certificateServiceId);


    }

    private Optional<String> extractFromSslInfo(ServerWebExchange exchange) {
        if (applicationInfo != null && applicationInfo.isModulith()) {
            return Optional.ofNullable(exchange.getRequest().getSslInfo())
                .map(SslInfo::getPeerCertificates)
                .filter(certs -> certs.length > 0)
                .map(certs -> certs[0].getSubjectX500Principal().getName());
        }
        return extractFromAttribute(exchange);
    }

    private Optional<String> extractFromAttribute(ServerWebExchange exchange) {
        return Optional.ofNullable((X509Certificate[]) exchange.getAttributes()
                .get(CategorizeCertsFilter.ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE))
            .filter(certs -> certs.length > 0)
            .map(certs -> certs[0].getSubjectX500Principal().getName());
    }

    private Optional<String> getHeader(ServerWebExchange exchange, String headerName) {
        String serviceId = exchange.getRequest().getHeaders().getFirst(headerName);
        if (StringUtils.isEmpty(serviceId)) {
            return Optional.empty();
        } else {
            return Optional.of(serviceId);
        }
    }

    private StorageException invalidPayloadException(String keyValue, String message) {
        return new StorageException(Messages.INVALID_PAYLOAD.getKey(), Messages.INVALID_PAYLOAD.getStatus(),
            keyValue, message);
    }

    private void checkForInvalidPayload(KeyValue keyValue) {
        if (keyValue == null) {
            throw invalidPayloadException(null, "No KeyValue provided in the payload");
        }

        if (keyValue.getValue() == null) {
            throw invalidPayloadException(keyValue.toString(), "No value provided in the payload");
        }

        if (keyValue.getKey() == null) {
            throw invalidPayloadException(keyValue.toString(), "No key provided in the payload");
        }
    }

    private Map<String, Collection<String>> checkQueryPayload(Map<String, List<String>> keysByMapKey) {
        if (keysByMapKey == null) {
            throw invalidPayloadException(null, "No map keys provided in the payload");
        }

        int total = 0;
        Map<String, Collection<String>> request = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : keysByMapKey.entrySet()) {
            if (entry.getKey() == null) {
                throw invalidPayloadException(keysByMapKey.toString(), "No map key provided in the payload");
            }
            List<String> keys = entry.getValue() == null ? List.of() : entry.getValue();
            total += keys.size();
            request.put(entry.getKey(), keys);
        }

        if (total > maxQueryKeys) {
            throw invalidPayloadException(String.valueOf(total),
                "Too many keys requested at once, the limit is " + maxQueryKeys);
        }
        return request;
    }

}
