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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@RestController
public class ZosmfController {
    @RequestMapping("/**")
    public ResponseEntity<?> zosmfCall(
        HttpServletRequest servletRequest,
        HttpServletResponse response,
        @RequestHeader Map<String, String> headers,
        HttpEntity<String> httpEntity
    ) {
        System.out.println(servletRequest.getServletPath() + " " + servletRequest.getMethod());
        if (headers.get("authorization") != null) {
            System.out.println("Authorization: " + headers.get("authorization"));
        }

        if (servletRequest.getServletPath().contains("/zosmf/services/authenticate")) {
            if (servletRequest.getMethod().equals("DELETE")) {
                System.out.println("Delete");
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            if (headers.get("authorization") == null) {
                System.out.println("Unauthorized");
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            if (
                headers.get("authorization").equals("Basic aW5jb3JyZWN0VXNlcjppbmNvcnJlY3RQYXNzd29yZA==")
            ) {
                System.out.println("Unauthorized");
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            System.out.println("Ok");
            return validJwtResponse(response);
        } else if (servletRequest.getServletPath().contains("/zosmf/info")) {
            return zosmfInfo(response);
        } else if (servletRequest.getServletPath().contains("/jwt/ibm/api/zOSMFBuilder/jwk")) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            // restfiles data information ?
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }

    private ResponseEntity<?> validJwtResponse(
        HttpServletResponse response
    ) {
        Date current = new Date();
        Date expiration = new Date(current.getTime() + 86400);

        String jwtToken = Jwts.builder()
            .setSubject("APIMTST")
            .setIssuedAt(new Date())
            .setExpiration(expiration)
            .setIssuer("zOSMF")
            .setId(UUID.randomUUID().toString())
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

    private ResponseEntity<?> zosmfInfo(
        HttpServletResponse response
    ) {
        Cookie ltpaToken = new Cookie("LtpaToken2", "paMypL7yRO/IBroQtro21/uSC2LTrJvOuYebHaPc6JAUNWQ7lEHHt1l3CYeXa/nP6aKLFHTuyWy3qlRXvt10PjVdVl+7Q+wavgIsro7odz+PvTaJBp/+r0AH+DHYcdZikKe8dytGYZRH2c2gw8Gv3PliDIMd1iPEazY4HeYTU5VCFM5cBJkeIoTXCfL5ud9wTzrkY2c4h1PQPtx+hYCF4kEpiVkqIypVwjQLzWdJGV1Ihz7NqH/UU9MMJRXY1xMqsWZSibs2fX5MVK77dnyBrNYjVXA7PqYL6U/v5/1UCvuYQ/iEU9+Uy95J+xFEsnTX");

        ltpaToken.setSecure(true);
        ltpaToken.setHttpOnly(true);
        ltpaToken.setPath("/");

        response.addCookie(ltpaToken);

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
