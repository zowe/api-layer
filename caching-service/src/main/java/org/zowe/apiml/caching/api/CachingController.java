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
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.service.ZaasClient;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
public class CachingController {
    //TODO what is no authorization? How separate from jwt invalid?
    //TODO hash key values to adjust for limit of 250 chars, and only use ascii alphanum

    private final Storage storage;
    private final ZaasClient zaasClient;
    private final MessageService messageService;

    @GetMapping(value = "/api/v1/cache/{key}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Retrieves a specific value in the cache",
        notes = "Value returned is for the provided {key}")
    @ResponseBody
    public ResponseEntity<?> getValue(@PathVariable String key, HttpServletRequest request) {
        //TODO 400 if no key
        //TODO 404 if key not in cache
        String serviceId = getServiceId();
        return new ResponseEntity<>(storage.read(serviceId, key), HttpStatus.OK);
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
        //TODO 400 - invalid json data, no json data
        //TODO 409 key already exists
        String serviceId = getServiceId();
        storage.create(serviceId, keyValue);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping(value = "/api/v1/cache/{key}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Update key in the cache",
        notes = "Value at the key in the provided key-value pair will be updated to the provided value")
    @ResponseBody
    public ResponseEntity<?> update(@RequestBody KeyValue keyValue, HttpServletRequest request) {
        //TODO 400 - no key, no authorization provided
        //TODO 404 key not in cache
        String serviceId = getServiceId();
        storage.update(serviceId, keyValue);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(value = "/api/v1/cache/{key}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Delete key from the cache",
        notes = "Will delete key-value pair for the provided {key}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable String key, HttpServletRequest request) {
        //TODO 400 - no key
        //TODO 404 key not in cache
        String serviceId = getServiceId();
        storage.delete(serviceId, key);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private String getServiceId() {
        return "test-service"; //TODO get from auth
    }
}
