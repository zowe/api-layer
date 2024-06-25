/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.services.apars;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@SuppressWarnings("squid:S1452")
public class PHBase extends FunctionalApar {

    public PHBase(List<String> usernames, List<String> passwords) {
        super(usernames, passwords);
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationCreate(Map<String, String> headers, HttpServletResponse response) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationVerify(Map<String, String> headers, HttpServletResponse response) {
        if (noAuthentication(headers)) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (containsInvalidOrNoUser(headers) && !ltpaIsPresent(headers)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        setLtpaToken(response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationDefault(Map<String, String> headers) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Override
    protected ResponseEntity<?> handleInformation(Map<String, String> headers, HttpServletResponse response) {
        if (containsInvalidOrNoUser(headers)) {
            return validInfo();
        }

        setLtpaToken(response);
        return validInfo();
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationDelete(Map<String, String> headers) {
        if (!validLtpaCookie(headers)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    protected ResponseEntity<?> handleFiles(Map<String, String> headers) {
        String authorization = headers.get(AUTHORIZATION_HEADER);

        if (authorization != null) {
            if (authorization.startsWith("Bearer")) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        } else {
            if (!isValidJwtCookie(headers) && !ltpaIsPresent(headers)) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }

        return datasets();
    }

    @SuppressWarnings("squid:S1192")
    private ResponseEntity<?> datasets() {
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

    private ResponseEntity<?> validInfo() {
        return new ResponseEntity<>("{\n" +
            "  \"zos_version\": \"04.27.00\",\n" +
            "  \"zosmf_port\": \"1443\",\n" +
            "  \"zosmf_version\": \"27\",\n" +
            "  \"zosmf_hostname\": \"usilca32.lvn.broadcom.net\",\n" +
            "  \"plugins\": {\n" +
            "    \"msgId\": \"IZUG612E\",\n" +
            "    \"msgText\": \"IZUG612E\"\n" +
            "  },\n" +
            "  \"zosmf_saf_realm\": \"SAFRealm\",\n" +
            "  \"zosmf_full_version\": \"27.0\",\n" +
            "  \"api_version\": \"1\"\n" +
            "}", HttpStatus.OK);
    }
}
