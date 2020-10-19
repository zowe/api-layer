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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.zaasclient.service.ZaasClient;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

@RestController
@RequiredArgsConstructor
public class CachingController {
    //TODO hash key values to adjust for limit of 250 chars, and only use ascii alphanum

    private final Storage storage;
    private final ZaasClient zaasClient;
    private final MessageService messageService;

    @GetMapping(value = "/api/v1/cache/{key}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Retrieves a specific value in the cache",
        notes = "Value returned is for the provided {key}")
    @ResponseBody
    public ResponseEntity<?> getValue(@PathVariable String key, HttpServletRequest request) {
        String serviceId = getServiceId();

        if (key == null) {
            return noKeyProvidedResponse(serviceId);
        }

        KeyValue readPair = storage.read(serviceId, key);

        if (readPair == null) {
            Message message = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", key, serviceId);
            return new ResponseEntity<>(message.mapToView(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(readPair, HttpStatus.OK);
    }

    @GetMapping(value = "/api/v1/cache", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Retrieves all values in the cache",
        notes = "Values returned for the calling service")
    @ResponseBody
    public ResponseEntity<?> getAllValues(HttpServletRequest request) {
        String serviceId = getServiceId();
        return new ResponseEntity<>(storage.readForService(serviceId), HttpStatus.OK);
    }

    @PostMapping(value = "/api/v1/cache", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Create a new key in the cache",
        notes = "A new key-value pair will be added to the cache")
    @ResponseBody
    public ResponseEntity<?> createKey(@RequestBody KeyValue keyValue, HttpServletRequest request) {
        if (isInvalidPayload(keyValue)) {
            return invalidPayloadResponse(keyValue);
        }

        String serviceId = getServiceId();
        KeyValue createdPair = storage.create(serviceId, keyValue);

        if (createdPair == null) {
            Message message = messageService.createMessage("org.zowe.apiml.cache.keyCollision", keyValue.getKey());
            return new ResponseEntity<>(message.mapToView(), HttpStatus.CONFLICT);
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping(value = "/api/v1/cache", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Update key in the cache",
        notes = "Value at the key in the provided key-value pair will be updated to the provided value")
    @ResponseBody
    public ResponseEntity<?> update(@RequestBody KeyValue keyValue, HttpServletRequest request) {
        if (isInvalidPayload(keyValue)) {
            return invalidPayloadResponse(keyValue);
        }

        String serviceId = getServiceId();
        KeyValue updatedPair = storage.update(serviceId, keyValue);

        if (updatedPair == null) {
            Message message = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", keyValue.getKey(), serviceId);
            return new ResponseEntity<>(message.mapToView(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(value = "/api/v1/cache/{key}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Delete key from the cache",
        notes = "Will delete key-value pair for the provided {key}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable String key, HttpServletRequest request) {
        String serviceId = getServiceId();

        if (key == null) {
            return noKeyProvidedResponse(serviceId);
        }

        KeyValue deletedPair = storage.delete(serviceId, key);

        if (deletedPair == null) {
            Message message = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", key, serviceId);
            return new ResponseEntity<>(message.mapToView(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private String getServiceId() {
        return "test-service"; //TODO get from auth
    }

    private boolean isInvalidPayload(KeyValue keyValue) {
        return keyValue == null; //TODO proper validation
    }

    private ResponseEntity<ApiMessageView> invalidPayloadResponse(KeyValue keyValue) {
        Message message = messageService.createMessage("org.zowe.apiml.cache.invalidPayload", keyValue);
        return new ResponseEntity<>(message.mapToView(), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ApiMessageView> noKeyProvidedResponse(String serviceId) {
        Message message = messageService.createMessage("org.zowe.apiml.cache.keyNotProvided", serviceId);
        return new ResponseEntity<>(message.mapToView(), HttpStatus.BAD_REQUEST);
    }
}
