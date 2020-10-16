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

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Storage;

@RestController
@RequiredArgsConstructor
public class CachingController {
    //TODO what is no authorization? How separate from jwt invalid?
    //TODO how to do authentication?
    //TODO hash key values to adjust for limit of 250 chars, and only use ascii alphanum

    private final Storage storage;

    @RequestMapping(value = "/api/v1/cache/{key}", produces = "application/json; charset=utf-8", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<?> getValue(@PathVariable String key) {
        //TODO 400 if no key
        //TODO 404 if key not in cache
        String serviceId = getServiceId();
        return new ResponseEntity<>(storage.read(serviceId, key), HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/cache", produces = "application/json; charset=utf-8", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<?> getAllValues() {
        String serviceId = getServiceId();
        return new ResponseEntity<>(storage.readForService(serviceId), HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/cache", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<?> createKey(@RequestBody KeyValue keyValue) {
        //TODO 400 - invalid json data, no json data
        //TODO 409 key already exists
        String serviceId = getServiceId();
        storage.create(serviceId, keyValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/cache/{key}", produces = "application/json; charset=utf-8", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<?> update(@RequestBody KeyValue keyValue) {
        //TODO 400 - no key, no authorization provided
        //TODO 404 key not in cache
        String serviceId = getServiceId();
        storage.update(serviceId, keyValue);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/api/v1/cache/{key}", produces = "application/json; charset=utf-8", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable String key) {
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
