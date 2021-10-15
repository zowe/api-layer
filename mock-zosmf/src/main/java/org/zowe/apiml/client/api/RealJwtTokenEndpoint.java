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

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.client.services.JwtTokenService;

import javax.servlet.http.HttpServletResponse;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
// TODO should be conditioned on something else
@ConditionalOnProperty(name = "jwtToken.enableMock", havingValue = "true")
public class RealJwtTokenEndpoint {

    private final JwtTokenService tokenService;

    @PostMapping(value = "/zosmf/services/authenticate", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> authenticate(
        HttpServletResponse response,
        @RequestHeader Map<String, String> headers
    ) throws NoSuchAlgorithmException, InvalidKeySpecException {

        if (headers.containsKey(HttpHeaders.AUTHORIZATION.toLowerCase()) && !headers.get(HttpHeaders.AUTHORIZATION.toLowerCase()).equals("Basic Og==")) {
            HttpHeaders resHeaders = new HttpHeaders();
            resHeaders.add(HttpHeaders.SET_COOKIE, "jwtToken=" + tokenService.generateJwt("USER"));
            return new ResponseEntity(resHeaders, HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }

    @DeleteMapping(value = "/zosmf/services/authenticate", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> logout(HttpServletResponse response,
                                    @RequestHeader Map<String, String> headers) {
        tokenService.invalidateJwtToken(extractToken(headers));
        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "*") // for https://token.dev/
    @GetMapping(value = "/jwt/ibm/api/zOSMFBuilder/jwk", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> jwk(HttpServletResponse response,
                                 @RequestHeader Map<String, String> headers) throws NoSuchAlgorithmException, InvalidKeySpecException {

        return ResponseEntity.of(Optional.of(JwtTokenService.getKeySet().toJSONObject()));
    }

    @GetMapping(value = "/zosmf/notifications/inbox", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> verify(HttpServletResponse response, @RequestHeader Map<String, String> headers) throws NoSuchAlgorithmException, InvalidKeySpecException {

        String token = extractToken(headers);

        if (token.isEmpty()) {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

        if (tokenService.validateJwtToken(token)) {
            return new ResponseEntity(HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

    }

    @GetMapping(value = "/zosmf/info", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> info(
        HttpServletResponse response,
        @RequestHeader Map<String, String> headers
    ) {
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

    private String extractToken(Map<String, String> headers) {

        return headers.entrySet().stream().filter(e -> e.getKey().equals("cookie") && e.getValue().startsWith("jwtToken="))
            .map(Map.Entry::getValue).map(s -> s.replaceFirst("jwtToken=", "")).findFirst().orElse("");
    }
}
