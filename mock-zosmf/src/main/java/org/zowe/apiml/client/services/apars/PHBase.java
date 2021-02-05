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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PHBase extends FunctionalApar {

    public PHBase(List<String> usernames, List<String> passwords) {
        super(usernames, passwords);
    }

    @Override
    protected Optional<ResponseEntity<?>> handleAuthenticationVerify(Map<String, String> headers, HttpServletResponse response) {
        String authorization = headers.get("authorization");
        if (containsInvalidUser(authorization) && noLtpaCookie(headers)) {
            return Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
        }

        setLtpaToken(response);
        return Optional.of(new ResponseEntity<>(HttpStatus.OK));
    }

    @Override
    protected Optional<ResponseEntity<?>> handleAuthenticationDefault(Map<String, String> headers) {
        return Optional.of(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    protected Optional<ResponseEntity<?>> handleInformation(Map<String, String> headers, HttpServletResponse response) {
        String authorization = headers.get("authorization");
        if (containsInvalidUser(authorization)) {
            return validInfo();
        }

        setLtpaToken(response);
        return validInfo();
    }

    @Override
    protected Optional<ResponseEntity<?>> handleFiles(Map<String, String> headers) {
        String authorization = headers.get("authorization");

        if (authorization != null) {
            if (authorization.startsWith("Bearer")) {
                return Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
            }
        } else {
            if (noLtpaCookie(headers)) {
                return Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
            }
        }

        return datasets();
    }

    private Optional<ResponseEntity<?>> datasets() {
        return Optional.of(new ResponseEntity<>("{\n" +
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
            "}", HttpStatus.OK));
    }

    private boolean noLtpaCookie(Map<String, String> headers) {
        String cookie = headers.get("cookie");
        return cookie == null || !cookie.contains("LtpaToken2");
    }

    private void setLtpaToken(HttpServletResponse response) {
        Cookie ltpaToken = new Cookie("LtpaToken2", "paMypL7yRO/IBroQtro21/uSC2LTrJvOuYebHaPc6JAUNWQ7lEHHt1l3CYeXa/nP6aKLFHTuyWy3qlRXvt10PjVdVl+7Q+wavgIsro7odz+PvTaJBp/+r0AH+DHYcdZikKe8dytGYZRH2c2gw8Gv3PliDIMd1iPEazY4HeYTU5VCFM5cBJkeIoTXCfL5ud9wTzrkY2c4h1PQPtx+hYCF4kEpiVkqIypVwjQLzWdJGV1Ihz7NqH/UU9MMJRXY1xMqsWZSibs2fX5MVK77dnyBrNYjVXA7PqYL6U/v5/1UCvuYQ/iEU9+Uy95J+xFEsnTX");

        ltpaToken.setSecure(true);
        ltpaToken.setHttpOnly(true);
        ltpaToken.setPath("/");

        response.addCookie(ltpaToken);
    }

    private Optional<ResponseEntity<?>> validInfo() {
        return Optional.of(new ResponseEntity<>("{\n" +
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
            "}", HttpStatus.OK));
    }
}
