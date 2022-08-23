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

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CachingController {
    private final Storage storage;
    private final MessageService messageService;


    @GetMapping(value = "/cache", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves all values in the cache",
        description = "Values returned for the calling service")
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<Object> getAllValues(HttpServletRequest request) {
        return getServiceId(request).<ResponseEntity<Object>>map(
            s -> {
                try {
                    return new ResponseEntity<>(storage.readForService(s), HttpStatus.OK);
                } catch (Exception exception) {
                    return handleInternalError(exception, request.getRequestURL());
                }
            }
        ).orElseGet(this::getUnauthorizedResponse);
    }

    @DeleteMapping(value = "/cache", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete all values for service from the cache",
        description = "Will delete all key-value pairs for specific service")
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<Object> deleteAllValues(HttpServletRequest request) {
        return getServiceId(request).map(
            s -> {
                try {
                    storage.deleteForService(s);
                    return new ResponseEntity<>(HttpStatus.OK);
                } catch (Exception exception) {
                    return handleInternalError(exception, request.getRequestURL());
                }
            }
        ).orElseGet(this::getUnauthorizedResponse);
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
    @HystrixCommand
    public ResponseEntity<Object> getValue(@PathVariable String key, HttpServletRequest request) {
        return keyRequest(storage::read,
            key, request, HttpStatus.OK);
    }

    @DeleteMapping(value = "/cache/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete key from the cache",
        description = "Will delete key-value pair for the provided {key}")
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<Object> delete(@PathVariable String key, HttpServletRequest request) {
        return keyRequest(storage::delete,
            key, request, HttpStatus.NO_CONTENT);
    }

    @PostMapping(value = "/cache", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new key in the cache",
        description = "A new key-value pair will be added to the cache")
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<Object> createKey(@RequestBody KeyValue keyValue, HttpServletRequest request) {
        return keyValueRequest(storage::create,
            keyValue, request, HttpStatus.CREATED);
    }

    @PostMapping(value = "/cache-list/{mapKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a new item in the cache map",
        description = "A new key-value pair will be added to the specific cache map with given map key.")
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<Object> storeMapItem(@PathVariable String mapKey, @RequestBody KeyValue keyValue, HttpServletRequest request) {
        return mapKeyValueRequest(storage::storeMapItem,
            mapKey, keyValue, request, HttpStatus.CREATED);
    }

    @GetMapping(value = "/cache-list/{mapKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves all the items in the cache map",
        description = "Values returned for the calling service and specific cache map.")
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<Object> getAllMapItems(@PathVariable String mapKey, HttpServletRequest request) {
        return getServiceId(request).<ResponseEntity<Object>>map(
            s -> {
                try {
                    return new ResponseEntity<>(storage.getAllMapItems(s, mapKey), HttpStatus.OK);
                } catch (Exception exception) {
                    return handleIncompatibleStorageMethod(exception, request.getRequestURL());
                }
            }
        ).orElseGet(this::getUnauthorizedResponse);
    }

    @GetMapping(value = "/cache-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves all the maps in the cache",
        description = "Values returned for the calling service")
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<Object> getAllMaps(HttpServletRequest request) {
        return getServiceId(request).<ResponseEntity<Object>>map(
            s -> {
                try {
                    return new ResponseEntity<>(storage.getAllMaps(s), HttpStatus.OK);
                } catch (Exception exception) {
                    return handleIncompatibleStorageMethod(exception, request.getRequestURL());
                }
            }
        ).orElseGet(this::getUnauthorizedResponse);
    }

    @DeleteMapping(value = "/cache-list/evict/rules/{mapKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a record from a rules map in the cache",
        description = "Will delete a key-value pair from a specific rules map")
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<Object> evictRules(@PathVariable String mapKey, HttpServletRequest request) {
        return getServiceId(request).map(
            s -> {
                try {
                    storage.removeNonRelevantRules(s, mapKey);
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                } catch (Exception exception) {
                    return handleInternalError(exception, request.getRequestURL());
                }
            }
        ).orElseGet(this::getUnauthorizedResponse);
    }

    @DeleteMapping(value = "/cache-list/evict/tokens/{mapKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a record from an invalid tokens map in the cache",
        description = "Will delete a key-value pair from a specific tokens map")
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<Object> evictTokens(@PathVariable String mapKey, HttpServletRequest request) {
        return getServiceId(request).map(
            s -> {
                try {
                    storage.removeNonRelevantTokens(s, mapKey);
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                } catch (Exception exception) {
                    return handleInternalError(exception, request.getRequestURL());
                }
            }
        ).orElseGet(this::getUnauthorizedResponse);
    }

    @PutMapping(value = "/cache", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update key in the cache",
        description = "Value at the key in the provided key-value pair will be updated to the provided value")
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<Object> update(@RequestBody KeyValue keyValue, HttpServletRequest request) {
        return keyValueRequest(storage::update,
            keyValue, request, HttpStatus.NO_CONTENT);
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
    private ResponseEntity<Object> keyRequest(KeyOperation keyOperation, String key, HttpServletRequest request, HttpStatus successStatus) {
        Optional<String> serviceId = getServiceId(request);
        if (!serviceId.isPresent()) {
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
            return handleInternalError(exception, request.getRequestURL());
        }
    }

    /**
     * Authenticate the user.
     * verify validity of the data.
     * Do the storage operation passed in as Lambda
     * Properly handle and package Exceptions.
     */
    private ResponseEntity<Object> keyValueRequest(KeyValueOperation keyValueOperation, KeyValue keyValue,
                                                   HttpServletRequest request, HttpStatus successStatus) {
        Optional<String> serviceId = getServiceId(request);
        if (!serviceId.isPresent()) {
            return getUnauthorizedResponse();
        }

        try {
            checkForInvalidPayload(keyValue);

            keyValueOperation.storageRequest(serviceId.get(), keyValue);

            return new ResponseEntity<>(successStatus);
        } catch (StorageException exception) {
            return exceptionToResponse(exception);
        } catch (Exception exception) {
            return handleInternalError(exception, request.getRequestURL());
        }
    }

    private ResponseEntity<Object> mapKeyValueRequest(MapKeyValueOperation operation, String mapKey, KeyValue keyValue,
                                                      HttpServletRequest request, HttpStatus successStatus) {
        Optional<String> serviceId = getServiceId(request);
        if (!serviceId.isPresent()) {
            return getUnauthorizedResponse();
        }

        try {
            checkForInvalidPayload(keyValue);

            operation.storageRequest(serviceId.get(), mapKey, keyValue);

            return new ResponseEntity<>(successStatus);
        } catch (StorageException exception) {
            return exceptionToResponse(exception);
        } catch (Exception exception) {
            return handleInternalError(exception, request.getRequestURL());
        }
    }

    private Optional<String> getServiceId(HttpServletRequest request) {
        Optional<String> certificateServiceId = getHeader(request, "X-Certificate-DistinguishedName");
        Optional<String> specificServiceId = getHeader(request, "X-CS-Service-ID");

        if (certificateServiceId.isPresent() && specificServiceId.isPresent()) {
            return Optional.of(certificateServiceId.get() + ", SERVICE=" + specificServiceId.get());
        } else if (!specificServiceId.isPresent()) {
            return certificateServiceId;
        } else {
            return specificServiceId;
        }
    }

    private Optional<String> getHeader(HttpServletRequest request, String headerName) {
        String serviceId = request.getHeader(headerName);
        if (StringUtils.isEmpty(serviceId)) {
            return Optional.empty();
        } else {
            return Optional.of(serviceId);
        }
    }

    private ResponseEntity<Object> handleInternalError(Exception exception, StringBuffer requestURL) {
        Messages internalServerError = Messages.INTERNAL_SERVER_ERROR;
        Message message = messageService.createMessage(internalServerError.getKey(), requestURL, exception.getMessage(), exception.toString());
        return new ResponseEntity<>(message.mapToView(), internalServerError.getStatus());
    }

    private ResponseEntity<Object> handleIncompatibleStorageMethod(Exception exception, StringBuffer requestURL) {
        Messages internalServerError = Messages.INCOMPATIBLE_STORAGE_METHOD;
        Message message = messageService.createMessage(internalServerError.getKey(), requestURL, exception.getMessage(), exception.toString());
        return new ResponseEntity<>(message.mapToView(), internalServerError.getStatus());
    }

    private void keyNotInCache() {
        throw new StorageException(Messages.KEY_NOT_PROVIDED.getKey(), Messages.KEY_NOT_PROVIDED.getStatus());
    }

    private void invalidPayload(String keyValue, String message) {
        throw new StorageException(Messages.INVALID_PAYLOAD.getKey(), Messages.INVALID_PAYLOAD.getStatus(),
            keyValue, message);
    }

    private void checkForInvalidPayload(KeyValue keyValue) {
        if (keyValue == null) {
            invalidPayload(null, "No KeyValue provided in the payload");
        }

        if (keyValue.getValue() == null) {
            invalidPayload(keyValue.toString(), "No value provided in the payload");
        }

        String key = keyValue.getKey();
        if (key == null) {
            invalidPayload(keyValue.toString(), "No key provided in the payload");
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
