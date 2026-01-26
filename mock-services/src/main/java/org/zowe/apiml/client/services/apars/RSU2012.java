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
import org.zowe.apiml.client.services.JwtTokenService;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@SuppressWarnings("squid:S1452")
public class RSU2012 extends FunctionalApar {
    private final String keystorePath;

    public RSU2012(List<String> usernames, List<String> passwords, String keystorePath, Integer timeout) {
        super(usernames, passwords, new JwtTokenService(timeout));
        this.keystorePath = keystorePath;
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationCreate(Map<String, String> headers, HttpServletResponse response) {
        // JWT token not accepted for create method
        if (containsInvalidOrNoUser(headers) && noLtpaCookie(headers)) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String[] credentials = getPiecesOfCredentials(headers);
        return validJwtResponse(response, credentials[0], keystorePath);
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationVerify(Map<String, String> headers, HttpServletResponse response) {
        if (containsInvalidOrNoUser(headers) && noLtpaCookie(headers) && !isValidJwtCookie(headers)) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String[] credentials = getPiecesOfCredentials(headers);
        return validJwtResponse(response, credentials[0], keystorePath);
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationDelete(Map<String, String> headers) {
        if (containsInvalidOrNoUser(headers) && noLtpaCookie(headers) && !isValidJwtCookie(headers)) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    protected ResponseEntity<?> handleInformation(Map<String, String> headers, HttpServletResponse response) {
        if (containsInvalidOrNoUser(headers)) {
            return validInfo();
        }

        setLtpaToken(response);
        return validInfo();
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
