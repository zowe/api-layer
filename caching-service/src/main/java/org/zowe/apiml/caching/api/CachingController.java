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
import org.zowe.apiml.caching.service.Storage;

@RestController
@RequiredArgsConstructor
public class CachingController {
    private final Storage storage;

    @RequestMapping(value = "/api/v1/cache/{key}", produces = "application/json; charset=utf-8", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<?> getKey(
        @PathVariable String key
    ) {
        String serviceId = "test-service";
        return new ResponseEntity<>(storage.read(serviceId, key), HttpStatus.OK);
    }

}
