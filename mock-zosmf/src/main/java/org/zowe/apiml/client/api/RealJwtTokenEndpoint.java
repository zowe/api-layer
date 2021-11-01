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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.client.services.JwtTokenService;

import javax.servlet.http.HttpServletResponse;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequiredArgsConstructor
@SuppressWarnings({"squid:S1452", "squid:S3740", "squid:S1192"})
@ConditionalOnProperty(name = "jwtToken.enableMock", havingValue = "true")
@Slf4j
public class RealJwtTokenEndpoint {

    private final JwtTokenService tokenService;

    @PostMapping(value = "/zosmf/services/authenticate", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> authenticate(
        HttpServletResponse response,
        @RequestHeader Map<String, String> headers
    ) {
        AtomicReference<ResponseEntity> returnValue = new AtomicReference<>(new ResponseEntity(HttpStatus.UNAUTHORIZED));
        Optional<Map.Entry<String, String>> basicAuthHeader = headers.entrySet().stream()
            .filter(e -> e.getKey().equalsIgnoreCase(HttpHeaders.AUTHORIZATION) && !e.getValue().equals("Basic Og=="))
            .findFirst();

        basicAuthHeader.ifPresent(h -> {
            HttpHeaders resHeaders = new HttpHeaders();
            try {
                resHeaders.add(HttpHeaders.SET_COOKIE, "jwtToken=" + tokenService.generateJwt("USER") + "; Path=/; Secure; HttpOnly");
            } catch (GeneralSecurityException e) {
                log.error("Failed to generate token", e);
            }
            returnValue.set(new ResponseEntity(resHeaders, HttpStatus.NO_CONTENT));
        });

        return returnValue.get();
    }

    @DeleteMapping(value = "/zosmf/services/authenticate", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> logout(HttpServletResponse response,
                                    @RequestHeader Map<String, String> headers) {
        tokenService.invalidateJwtToken(tokenService.extractToken(headers));
        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "*") //NOSONAR for https://token.dev/, this is not security hotspot
    @GetMapping(value = "/jwt/ibm/api/zOSMFBuilder/jwk", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> jwk(HttpServletResponse response,
                                 @RequestHeader Map<String, String> headers) throws NoSuchAlgorithmException, InvalidKeySpecException {

        return ResponseEntity.of(Optional.of(JwtTokenService.getKeySet().toJSONObject()));
    }

    @GetMapping(value = "/zosmf/notifications/inbox", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> verify(HttpServletResponse response, @RequestHeader Map<String, String> headers) {

        String token = tokenService.extractToken(headers);

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
            "  \"zosmf_hostname\": \"zosmf.host.name.net\",\n" +
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
