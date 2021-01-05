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

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Import;
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
import org.zowe.apiml.zaasclient.config.DefaultZaasClientConfiguration;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.service.ZaasClient;
import org.zowe.apiml.zaasclient.service.ZaasToken;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Import(DefaultZaasClientConfiguration.class)
public class CachingController {
    private final Storage storage;
    private final ZaasClient zaasClient;
    private final MessageService messageService;

    @GetMapping(value = "/cache", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Retrieves all values in the cache",
        notes = "Values returned for the calling service")
    @ResponseBody
    public ResponseEntity<Object> getAllValues(HttpServletRequest request) {
        String serviceId;
        try {
            serviceId = authenticate(request);
        } catch (ZaasClientException e) {
            return handleZaasClientException(e, request);
        }

        return new ResponseEntity<>(storage.readForService(serviceId), HttpStatus.OK);
    }

    @GetMapping(value = "/cache/{key}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Retrieves a specific value in the cache",
        notes = "Value returned is for the provided {key}")
    @ResponseBody
    public ResponseEntity<Object> getValue(@PathVariable String key, HttpServletRequest request) {
        return keyRequest(storage::read,
            key, request, HttpStatus.OK);
    }

    @DeleteMapping(value = "/cache/{key}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Delete key from the cache",
        notes = "Will delete key-value pair for the provided {key}")
    @ResponseBody
    public ResponseEntity<Object> delete(@PathVariable String key, HttpServletRequest request) {
        return keyRequest(storage::delete,
            key, request, HttpStatus.NO_CONTENT);
    }

    @PostMapping(value = "/cache", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Create a new key in the cache",
        notes = "A new key-value pair will be added to the cache")
    @ResponseBody
    public ResponseEntity<Object> createKey(@RequestBody KeyValue keyValue, HttpServletRequest request) {
        return keyValueRequest(storage::create,
            keyValue, request, HttpStatus.CREATED);
    }

    @PutMapping(value = "/cache", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Update key in the cache",
        notes = "Value at the key in the provided key-value pair will be updated to the provided value")
    @ResponseBody
    public ResponseEntity<Object> update(@RequestBody KeyValue keyValue, HttpServletRequest request) {
        return keyValueRequest(storage::update,
            keyValue, request, HttpStatus.NO_CONTENT);
    }

    private String authenticate(HttpServletRequest request) throws ZaasClientException {
        ZaasToken token = zaasClient.query(request);
        return token.getUserId();
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
        String serviceId;
        try {
            serviceId = authenticate(request);
        } catch (ZaasClientException e) {
            return handleZaasClientException(e, request);
        }

        try {
            if (key == null) {
                keyNotInCache();
            }

            KeyValue pair = keyOperation.storageRequest(serviceId, key);

            return new ResponseEntity<>(pair, successStatus);
        } catch (StorageException exception) {
            return exceptionToResponse(exception);
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
        String serviceId;
        try {
            serviceId = authenticate(request);
        } catch (ZaasClientException e) {
            return handleZaasClientException(e, request);
        }

        try {
            checkForInvalidPayload(keyValue);

            keyValueOperation.storageRequest(serviceId, keyValue);

            return new ResponseEntity<>(successStatus);
        } catch (StorageException exception) {
            return exceptionToResponse(exception);
        }
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
        if (!StringUtils.isAlphanumeric(key)) {
            invalidPayload(keyValue.toString(), "Key is not alphanumeric");
        }
    }

    private ResponseEntity<Object> handleZaasClientException(ZaasClientException e, HttpServletRequest request) {
        String requestUrl = request.getRequestURL().toString();
        Message message;
        HttpStatus statusCode;

        switch (e.getErrorCode()) {
            case TOKEN_NOT_PROVIDED:
                statusCode = HttpStatus.BAD_REQUEST;
                message = messageService.createMessage("org.zowe.apiml.security.query.tokenNotProvided", requestUrl);
                break;
            case INVALID_JWT_TOKEN:
                statusCode = HttpStatus.UNAUTHORIZED;
                message = messageService.createMessage("org.zowe.apiml.security.query.invalidToken", requestUrl);
                break;
            case EXPIRED_JWT_EXCEPTION:
                statusCode = HttpStatus.UNAUTHORIZED;
                message = messageService.createMessage("org.zowe.apiml.security.expiredToken", requestUrl);
                break;
            case SERVICE_UNAVAILABLE:
                statusCode = HttpStatus.NOT_FOUND;
                message = messageService.createMessage("org.zowe.apiml.cache.gatewayUnavailable", requestUrl, e.getMessage());
                break;
            default:
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
                message = messageService.createMessage("org.zowe.apiml.common.internalRequestError", requestUrl, e.getMessage(), e.getCause());
                break;
        }

        return new ResponseEntity<>(message.mapToView(), statusCode);
    }

    @FunctionalInterface
    interface KeyOperation {
        KeyValue storageRequest(String serviceId, String key);
    }

    @FunctionalInterface
    interface KeyValueOperation {
        KeyValue storageRequest(String serviceId, KeyValue keyValue);
    }
}
