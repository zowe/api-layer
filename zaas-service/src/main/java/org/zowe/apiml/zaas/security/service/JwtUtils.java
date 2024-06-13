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

import io.jsonwebtoken.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@UtilityClass
public class JwtUtils {

    private static final String HEADER_NONE_SIGNATURE = Base64.getEncoder().encodeToString("""
        {"typ":"JWT","alg":"none"}""".getBytes(StandardCharsets.UTF_8));

    private static final String TOKEN_IS_NOT_VALID_DUE_TO = "Token is not valid due to: {}.";

    /**
     * This method reads the claims without validating the token signature. It should be used only if the validity was checked in the calling code.
     *
     * @param jwt token to be parsed
     * @return parsed claims or empty object if the jwt is null
     * @throws TokenNotValidException in case of invalid input, or TokenExpireException if JWT is expired
     */
    public static Claims getJwtClaims(String jwt) {
        /*
         * Removes signature, because we don't have key to verify z/OS tokens, and we just need to read claim.
         * Verification is done by SAF itself. JWT library doesn't parse signed key without verification.
         */
        try {
            String withoutSign = removeJwtSign(jwt);
            return Jwts.parser().unsecured().build()
                .parseUnsecuredClaims(withoutSign)
                .getPayload();
        } catch (RuntimeException exception) {
            throw handleJwtParserException(exception);
        }
    }

    /**
     * This method removes the token signature and replace algorithm with none. It allows to parse payload without
     * public key.
     *
     * @param jwtToken token to modify
     * @return unsigned jwt token
     */
    public static String removeJwtSign(String jwtToken) {
        if (jwtToken == null) return null;

        int firstDot = jwtToken.indexOf('.');
        int lastDot = jwtToken.lastIndexOf('.');
        if ((firstDot < 0) || (firstDot >= lastDot)) throw new MalformedJwtException("Invalid JWT format");

        return HEADER_NONE_SIGNATURE + jwtToken.substring(firstDot, lastDot + 1);
    }

    /**
     * Method to translate original exception to internal one. It is used in case of parsing and verifying of JWT tokens.
     *
     * @param exception original exception
     * @return translated exception (better messaging and allow subsequent handling)
     */
    public static RuntimeException handleJwtParserException(RuntimeException exception) {
        if (exception instanceof ExpiredJwtException expiredJwtException) {
            log.debug("Token with id '{}' for user '{}' is expired.", expiredJwtException.getClaims().getId(), expiredJwtException.getClaims().getSubject());
            return new TokenExpireException("Token is expired.", exception);
        }
        if (exception instanceof JwtException) {
            log.debug(TOKEN_IS_NOT_VALID_DUE_TO, exception.getMessage());
            return new TokenNotValidException("Token is not valid.", exception);
        }

        log.debug(TOKEN_IS_NOT_VALID_DUE_TO, exception.getMessage());
        return new TokenNotValidException("An internal error occurred while validating the token therefore the token is no longer valid.", exception);
    }

}
