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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.cache.Storage;
import org.zowe.apiml.cache.StorageException;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.config.ApplicationInfo;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/cachingservice/api/v1")
public class CachingController {
    private final Storage storage;
    private final MessageService messageService;

    @Autowired(required = false)
    ApplicationInfo applicationInfo;


    @GetMapping(value = {"/cache", "/cache/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves all values in the cache",
        description = "Values returned for the calling service")
    @ResponseBody
    public Mono<ResponseEntity<Object>> getAllValues(ServerHttpRequest request) {
        return Mono.fromCallable(() -> getServiceId(request).<ResponseEntity<Object>>map(
            s -> {
                try {
                    return new ResponseEntity<>(storage.readForService(s), HttpStatus.OK);
                } catch (Exception exception) {
                    return handleInternalError(exception, request);
                }
            }
        ).orElseGet(this::getUnauthorizedResponse));
    }

    @DeleteMapping(value = {"/cache", "/cache/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete all values for service from the cache",
        description = "Will delete all key-value pairs for specific service")
    public Mono<ResponseEntity<Object>> deleteAllValues(ServerHttpRequest request) {
        return Mono.fromCallable(() -> getServiceId(request).map(
            s -> {
                try {
                    storage.deleteForService(s);
                    return new ResponseEntity<>(HttpStatus.OK);
                } catch (Exception exception) {
                    return handleInternalError(exception, request);
                }
            }
        ).orElseGet(this::getUnauthorizedResponse));
    }

    private ResponseEntity<Object> getUnauthorizedResponse() {
        Messages missingCert = Messages.MISSING_CERTIFICATE;
        Message message = messageService.createMessage(missingCert.getKey(), "parameter");
        return new ResponseEntity<>(message.mapToView(), missingCert.getStatus());
    }

    @GetMapping(value = "/cache/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves a specific value in the cache",
        description = "Value returned is for the provided {key}")
    @ResponseBody
    public Mono<ResponseEntity<Object>> getValue(@PathVariable String key, ServerHttpRequest request) {
        return Mono.fromCallable(() -> keyRequest(storage::read,
            key, request, HttpStatus.OK));
    }

    @DeleteMapping(value = "/cache/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete key from the cache",
        description = "Will delete key-value pair for the provided {key}")
    public Mono<ResponseEntity<Object>> delete(@PathVariable String key, ServerHttpRequest request) {
        return Mono.fromCallable(() -> keyRequest(storage::delete,
            key, request, HttpStatus.NO_CONTENT));
    }

    @PostMapping(value = {"/cache", "/cache/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new key in the cache",
        description = "A new key-value pair will be added to the cache")
    public Mono<ResponseEntity<Object>> createKey(@RequestBody KeyValue keyValue, ServerHttpRequest request) {
        return Mono.fromCallable(() -> keyValueRequest(storage::create,
            keyValue, request, HttpStatus.CREATED));
    }

    @PostMapping(value = "/cache-list/{mapKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a new item in the cache map",
        description = "A new key-value pair will be added to the specific cache map with given map key.")
    public Mono<ResponseEntity<Object>> storeMapItem(@PathVariable String mapKey, @RequestBody KeyValue keyValue, ServerHttpRequest request) {
        return Mono.fromCallable(() -> mapKeyValueRequest(storage::storeMapItem,
            mapKey, keyValue, request, HttpStatus.CREATED));
    }

    @GetMapping(value = "/cache-list/{mapKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves all the items in the cache map",
        description = "Values returned for the calling service and specific cache map.")
    @ResponseBody
    public Mono<ResponseEntity<Object>> getAllMapItems(@PathVariable String mapKey, ServerHttpRequest request) {
        return Mono.fromCallable(() -> getServiceId(request).<ResponseEntity<Object>>map(
            s -> {
                log.debug("Storing for serviceId: {}", s);
                try {
                    return new ResponseEntity<>(storage.getAllMapItems(s, mapKey), HttpStatus.OK);
                } catch (Exception exception) {
                    return handleIncompatibleStorageMethod(exception, request);
                }
            }
        ).orElseGet(this::getUnauthorizedResponse));
    }

    @GetMapping(value = {"/cache-list", "/cache-list/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves all the maps in the cache",
        description = "Values returned for the calling service")
    @ResponseBody
    public Mono<ResponseEntity<Object>> getAllMaps(ServerHttpRequest request) {
        return Mono.fromCallable(() -> getServiceId(request).<ResponseEntity<Object>>map(
            s -> {
                log.debug("Get all for serviceId: {}", s);
                try {
                    return new ResponseEntity<>(storage.getAllMaps(s), HttpStatus.OK);
                } catch (Exception exception) {
                    return handleIncompatibleStorageMethod(exception, request);
                }
            }
        ).orElseGet(this::getUnauthorizedResponse));
    }

    @DeleteMapping(value = "/cache-list/evict/rules/{mapKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a record from a rules map in the cache",
        description = "Will delete a key-value pair from a specific rules map")
    public Mono<ResponseEntity<Object>> evictRules(@PathVariable String mapKey, ServerHttpRequest request) {
        return Mono.fromCallable(() -> getServiceId(request).map(
            s -> {
                log.debug("Delete record for serviceId: {}", s);
                try {
                    storage.removeNonRelevantRules(s, mapKey);
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                } catch (Exception exception) {
                    return handleInternalError(exception, request);
                }
            }
        ).orElseGet(this::getUnauthorizedResponse));
    }

    @DeleteMapping(value = "/cache-list/evict/tokens/{mapKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a record from an invalid tokens map in the cache",
        description = "Will delete a key-value pair from a specific tokens map")
    public Mono<ResponseEntity<Object>> evictTokens(@PathVariable String mapKey, ServerHttpRequest request) {
        return Mono.fromCallable(() -> getServiceId(request).map(
            s -> {
                log.debug("Evict tokens for serviceId: {}", s);
                try {
                    storage.removeNonRelevantTokens(s, mapKey);
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                } catch (Exception exception) {
                    return handleInternalError(exception, request);
                }
            }
        ).orElseGet(this::getUnauthorizedResponse));
    }

    @PutMapping(value = {"/cache", "/cache/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update key in the cache",
        description = "Value at the key in the provided key-value pair will be updated to the provided value")
    public Mono<ResponseEntity<Object>> update(@RequestBody KeyValue keyValue, ServerHttpRequest request) {
        return Mono.fromCallable(() -> keyValueRequest(storage::update,
            keyValue, request, HttpStatus.NO_CONTENT));
    }


    private ResponseEntity<Object> exceptionToResponse(StorageException exception) {
        Message message = messageService.createMessage(exception.getKey(), (Object[]) exception.getParameters());
        return new ResponseEntity<>(message.mapToView(), exception.getStatus());
    }

    /**
     * Authenticate the user.
     * Verify validity of the data
     * Do the storage operation passed in as Lambda
     * Properly handle and package Exceptions.
     */
    private ResponseEntity<Object> keyRequest(KeyOperation keyOperation, String key, ServerHttpRequest request, HttpStatus successStatus) {
        Optional<String> serviceId = getServiceId(request);
        if (serviceId.isEmpty()) {
            return getUnauthorizedResponse();
        }
        try {
            if (key == null) {
                keyNotInCache();
            }

            KeyValue pair = keyOperation.storageRequest(serviceId.get(), key);

            return new ResponseEntity<>(pair, successStatus);
        } catch (StorageException exception) {
            return exceptionToResponse(exception);
        } catch (Exception exception) {
            return handleInternalError(exception, request);
        }
    }

    /**
     * Authenticate the user.
     * verify validity of the data.
     * Do the storage operation passed in as Lambda
     * Properly handle and package Exceptions.
     */
    private ResponseEntity<Object> keyValueRequest(KeyValueOperation keyValueOperation, KeyValue keyValue,
                                                   ServerHttpRequest request, HttpStatus successStatus) {
        Optional<String> serviceId = getServiceId(request);
        if (serviceId.isEmpty()) {
            return getUnauthorizedResponse();
        }

        try {
            checkForInvalidPayload(keyValue);

            keyValueOperation.storageRequest(serviceId.get(), keyValue);

            return new ResponseEntity<>(successStatus);
        } catch (StorageException exception) {
            return exceptionToResponse(exception);
        } catch (Exception exception) {
            return handleInternalError(exception, request);
        }
    }

    private ResponseEntity<Object> mapKeyValueRequest(MapKeyValueOperation operation, String mapKey, KeyValue keyValue,
                                                      ServerHttpRequest request, HttpStatus successStatus) {
        Optional<String> serviceId = getServiceId(request);
        if (serviceId.isEmpty()) {
            return getUnauthorizedResponse();
        }

        try {
            log.debug("All map for serviceId: {}", serviceId.get());
            checkForInvalidPayload(keyValue);

            operation.storageRequest(serviceId.get(), mapKey, keyValue);

            return new ResponseEntity<>(successStatus);
        } catch (StorageException exception) {
            return exceptionToResponse(exception);
        } catch (Exception exception) {
            return handleInternalError(exception, request);
        }
    }

    private Optional<String> getServiceId(ServerHttpRequest request) {
        Optional<String> certificateServiceId = getCertificateServiceId(request);
        Optional<String> specificServiceId = getHeader(request, "X-CS-Service-ID");

        if (certificateServiceId.isPresent() && specificServiceId.isPresent()) {
            return Optional.of(certificateServiceId.get() + ", SERVICE=" + specificServiceId.get());
        }

        return specificServiceId.or(() -> certificateServiceId);
    }

    private Optional<String> getCertificateServiceId(ServerHttpRequest request) {
        if (applicationInfo != null && applicationInfo.isModulith()) {
            return extractFromSslInfo(request);
        } else {
            return getHeader(request, "X-Certificate-DistinguishedName");
        }
    }

    private Optional<String> extractFromSslInfo(ServerHttpRequest request) {
        return Optional.ofNullable(request.getSslInfo())
            .map(SslInfo::getPeerCertificates)
            .filter(certs -> certs.length > 0)
            .map(certs -> certs[0].getSubjectX500Principal().getName());
    }

    private Optional<String> getHeader(ServerHttpRequest request, String headerName) {
        String serviceId = request.getHeaders().getFirst(headerName);
        if (StringUtils.isEmpty(serviceId)) {
            return Optional.empty();
        } else {
            return Optional.of(serviceId);
        }
    }

    private ResponseEntity<Object> handleInternalError(Exception exception, ServerHttpRequest request) {
        Messages internalServerError = Messages.INTERNAL_SERVER_ERROR;
        Message message = messageService.createMessage(internalServerError.getKey(), request.getURI().toString(), exception.getMessage(), exception.toString());
        return new ResponseEntity<>(message.mapToView(), internalServerError.getStatus());
    }

    private ResponseEntity<Object> handleIncompatibleStorageMethod(Exception exception, ServerHttpRequest request) {
        Messages internalServerError = Messages.INCOMPATIBLE_STORAGE_METHOD;
        Message message = messageService.createMessage(internalServerError.getKey(), request.getURI().toString(), exception.getMessage(), exception.toString());
        return new ResponseEntity<>(message.mapToView(), internalServerError.getStatus());
    }

    private void keyNotInCache() {
        throw new StorageException(Messages.KEY_NOT_PROVIDED.getKey(), Messages.KEY_NOT_PROVIDED.getStatus());
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

        String key = keyValue.getKey();
        if (key == null) {
            throw invalidPayloadException(keyValue.toString(), "No key provided in the payload");
        }
    }

    @FunctionalInterface
    interface KeyOperation {
        KeyValue storageRequest(String serviceId, String key);
    }

    @FunctionalInterface
    interface KeyValueOperation {
        KeyValue storageRequest(String serviceId, KeyValue keyValue) throws StorageException;
    }

    @FunctionalInterface
    interface MapKeyValueOperation {
        KeyValue storageRequest(String serviceId, String mapKey, KeyValue keyValue);
    }
}
