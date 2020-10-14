/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FilesController {
    @RequestMapping(value = "/zosmf/restfiles/ds", produces = "application/json; charset=utf-8", method = RequestMethod.GET)
    public ResponseEntity<?> readFiles(
        @RequestHeader Map<String, String> headers
    ) {
        String authorization = headers.get("authorization");

        if (authorization != null) {
            if (authorization.startsWith("Bearer")) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        } else {
            if (headers.get("cookie") == null || !headers.get("cookie").contains("LtpaToken2")) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }

        return new ResponseEntity<>("{\n" +
            "  \"items\": [\n" +
            "    {\n" +
            "      \"dsname\": \"SYS1.PAGEDUMP.VMVD21M\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"dsname\": \"SYS1.PAGEDUMP.VMVD22M\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"dsname\": \"SYS1.PAGEDUMP.VMVD23M\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"dsname\": \"SYS1.PAGEDUMP.VMVD24M\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"dsname\": \"SYS1.PARMLIB\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"dsname\": \"SYS1.PARMLIB.ARCHIVE\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"dsname\": \"SYS1.PARMLIB.D200328\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"dsname\": \"SYS1.PARMLIBN\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"dsname\": \"SYS1.PDEFLIB\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"dsname\": \"SYS1.PHELP\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"dsname\": \"SYS1.PROCLIB\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"dsname\": \"SYS1.PROCLIBX\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"dsname\": \"SYS1.PSEGLIB\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"returnedRows\": 13,\n" +
            "  \"JSONversion\": 1\n" +
            "}", HttpStatus.OK);
    }
}

