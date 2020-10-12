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

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class AuthenticationController {
    @Value("${zosmf.username}")
    private String[] usernames;
    @Value("${zosmf.password}")
    private String[] passwords;

    @RequestMapping(value = "/zosmf/services/authenticate", produces = "application/json; charset=utf-8", method = RequestMethod.DELETE)
    public ResponseEntity<?> logout() {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/zosmf/services/authenticate", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
    public ResponseEntity<?> authenticate(
        HttpServletResponse response,
        @RequestHeader Map<String, String> headers
    ) {
        String authorization = headers.get("authorization");

        if (authorization == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        if (
            authorization.equals("Basic aW5jb3JyZWN0VXNlcjppbmNvcnJlY3RQYXNzd29yZA==")
        ) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        byte[] decoded = Base64.getDecoder().decode(authorization.replace("Basic ", ""));
        String credentials = new String(decoded);
        String[] piecesOfCredentials = credentials.split(":");

        List<String> usernames = Arrays.asList(this.usernames);
        List<String> passwords = Arrays.asList(this.passwords);

        if(usernames.contains(piecesOfCredentials[0]) &&
            (passwords.contains(piecesOfCredentials[1]) || piecesOfCredentials[1].contains("PASS_TICKET"))
        ) {
            return validJwtResponse(response, piecesOfCredentials[0]);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/jwt/ibm/api/zOSMFBuilder/**", produces = "application/json; charset=utf-8", method = RequestMethod.GET)
    public ResponseEntity<?> jwk() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ResponseEntity<?> validJwtResponse(
        HttpServletResponse response,
        String username
    ) {
        Date current = new Date();
        Date expiration = new Date(current.getTime() + 86400);

        String jwtToken = Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(expiration)
            .setIssuer("zOSMF")
            .setId(UUID.randomUUID().toString())
            .signWith(getKeyForSigning())
            .compact();

        // Build a valid JWT token
        Cookie jwtCookie = new Cookie("jwtToken", jwtToken);
        jwtCookie.setSecure(true);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");

        response.addCookie(jwtCookie);

        Cookie ltpaToken = new Cookie("LtpaToken2", "paMypL7yRO/IBroQtro21/uSC2LTrJvOuYebHaPc6JAUNWQ7lEHHt1l3CYeXa/nP6aKLFHTuyWy3qlRXvt10PjVdVl+7Q+wavgIsro7odz+PvTaJBp/+r0AH+DHYcdZikKe8dytGYZRH2c2gw8Gv3PliDIMd1iPEazY4HeYTU5VCFM5cBJkeIoTXCfL5ud9wTzrkY2c4h1PQPtx+hYCF4kEpiVkqIypVwjQLzWdJGV1Ihz7NqH/UU9MMJRXY1xMqsWZSibs2fX5MVK77dnyBrNYjVXA7PqYL6U/v5/1UCvuYQ/iEU9+Uy95J+xFEsnTX");

        ltpaToken.setSecure(true);
        ltpaToken.setHttpOnly(true);
        ltpaToken.setPath("/");

        response.addCookie(ltpaToken);
        response.setContentType("application/json");
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }

    private Key getKeyForSigning() {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(
                new FileInputStream(new File("keystore/localhost/localhost.keystore.p12")),
                "password".toCharArray()
            );
            return ks.getKey("localhost", "password".toCharArray());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
