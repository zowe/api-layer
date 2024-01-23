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
import org.springframework.util.StringUtils;
import org.zowe.apiml.client.model.LoginBody;
import org.zowe.apiml.client.services.JwtTokenService;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@SuppressWarnings("squid:S1452")
public class PH34912 extends FunctionalApar {
    private final String keystorePath;

    public PH34912(List<String> usernames, List<String> passwords, String keystorePath, Integer timeout) {
        super(usernames, passwords, new JwtTokenService(timeout));
        this.keystorePath = keystorePath;
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationCreate(Map<String, String> headers, HttpServletResponse response) {
        if (noAuthentication(headers)) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (isUnauthorized(headers)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String[] credentials = getPiecesOfCredentials(headers);
        return validJwtResponse(response, credentials[0], keystorePath);
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationVerify(Map<String, String> headers, HttpServletResponse response) {
        return handleAuthenticationCreate(headers, response);
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationDelete(Map<String, String> headers) {
        if (noAuthentication(headers)) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (isUnauthorized(headers)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    protected ResponseEntity<?> handleJwtKeys() {
        return new ResponseEntity<>("{\n" +
            "  \"keys\": [\n" +
            "    {\n" +
            "      \"kty\": \"RSA\",\n" +
            "      \"e\": \"AQAB\",\n" +
            "      \"use\": \"sig\",\n" +
            "      \"kid\": \"ozG_ySMHRsVQFmN1mVBeS-WtCupY1r-K7ewben09IBg\",\n" +
            "      \"alg\": \"RS256\",\n" +
            "      \"n\": \"wRdwksGIAR2A4cHsoOsYcGp5AmQl5ZjF5xIPXeyjkaLHmNTMvjixdWso1ecVlVeg_6pIXzMRhmOvmjXjz1PLfI2GD3drmeqsStjISWdDfH_rIQCYc9wYbWIZ3bQ0wFRDaVpZ6iOZ2iNcIevvZQKNw9frJthKSMM52JtsgwrgN--Ub2cKWioU_d52SC2SfDzOdnChqlU7xkqXwKXSUqcGM92A35dJJXkwbZhAHnDy5FST1HqYq27MOLzBkChw1bJQHZtlSqkxcHPxphnnbFKQmwRVUvyC5kfBemX-7Mzp1wDogt5lGvBAf3Eq8rFxaevAke327rM7q2KqO_LDMN2J-Q\"\n" +
            "    }\n" +
            "  ]\n" +
            "}", HttpStatus.OK);
    }

    private boolean isUnauthorized(Map<String, String> headers) {
        return containsInvalidOrNoUser(headers) && noLtpaCookie(headers);
    }

    private boolean isInternalError(LoginBody body) {
        return !StringUtils.isEmpty(body.getOldPwd()) &&
            !StringUtils.isEmpty(body.getNewPwd()) && body.getOldPwd().equals(body.getNewPwd());
    }

    private boolean isBadRequest(LoginBody body) {
        return body == null ||
            (!passwords.contains(body.getOldPwd())) ||
            StringUtils.isEmpty(body.getUserID()) ||
            StringUtils.isEmpty(body.getOldPwd()) ||
            StringUtils.isEmpty(body.getNewPwd()) ||
            body.getOldPwd().equals(body.getNewPwd());
    }

    @Override
    protected ResponseEntity<?> handleChangePassword(LoginBody body) {
        if (isInternalError(body)) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (isBadRequest(body)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } else {
            passwords.add(body.getNewPwd());
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }
}
