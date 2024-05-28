/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

@Slf4j
@UtilityClass
public class JwtUtils {

    private static final String TOKEN_IS_NOT_VALID_DUE_TO = "Token is not valid due to: {}.";

    public static Claims getJwtClaims(String jwt) {
        /*
         * Removes signature, because we don't have key to verify z/OS tokens, and we just need to read claim.
         * Verification is done by SAF itself. JWT library doesn't parse signed key without verification.
         */
        String withoutSign = removeJwtSign(jwt);

        try {
            return Jwts.parserBuilder()
                    .build()
                    .parseClaimsJwt(withoutSign)
                    .getBody();
        } catch (RuntimeException exception) {
            throw handleJwtParserException(exception);
        }
    }

    /**
     * This method removes the token signature. Each JWT token is concatenated of three parts (header, body, sign) joined
     * with ".". JWT library used for parsing contains also validation. A public key is needed for validation, but
     * we are also using JWT tokens from another application (z/OSMF) and we don't have it.
     *
     * @param jwtToken token to modify
     * @return jwt token without sign part
     */
    public static String removeJwtSign(String jwtToken) {
        if (jwtToken == null) return null;

        final int index = jwtToken.lastIndexOf('.');
        if (index > 0) return jwtToken.substring(0, index + 1);

        return jwtToken;
    }

    /**
     * Method to translate original exception to internal one. It is used in case of parsing and verifying of JWT tokens.
     *
     * @param exception original exception
     * @return translated exception (better messaging and allow subsequent handling)
     */
    public static RuntimeException handleJwtParserException(RuntimeException exception) {
        if (exception instanceof ExpiredJwtException) {
            final ExpiredJwtException expiredJwtException = (ExpiredJwtException) exception;
            log.debug("Token with id '{}' for user '{}' is expired.", expiredJwtException.getClaims().getId(), expiredJwtException.getClaims().getSubject());
            return new TokenExpireException("Token is expired.");
        }
        if (exception instanceof JwtException) {
            log.debug(TOKEN_IS_NOT_VALID_DUE_TO, exception.getMessage());
            return new TokenNotValidException("Token is not valid.");
        }

        log.debug(TOKEN_IS_NOT_VALID_DUE_TO, exception.getMessage());
        return new TokenNotValidException("An internal error occurred while validating the token therefore the token is no longer valid.");
    }

}
