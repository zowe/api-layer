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
import io.jsonwebtoken.MalformedJwtException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenFormatNotValidException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

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

    /**
     * Extracts value of a field from an OIDC token. The value is extracted from a custom path which supports nested objects.
     * @param token to extract the field from
     * @param pathToField list of strings representing path to the field
     * @return userId extracted from the token
     *
     * @throws TokenFormatNotValidException in case of the field value cannot be extracted from the token, is null, or empty
     */
    @SuppressWarnings("rawtypes")
    public static String getFieldValueFromToken(String token, List<String> pathToField) throws TokenFormatNotValidException {
        if (token == null || pathToField == null || pathToField.isEmpty() || StringUtils.isBlank(pathToField.get(0))) {
            throw new IllegalArgumentException("Token and field path most not be null or empty");
        }

        try {
            Claims claims = getJwtClaims(token);
            String fieldValue;
            if (pathToField.size() == 1) {
                fieldValue = claims.get(pathToField.get(0), String.class);
            } else {
                var iterator = pathToField.iterator();
                var key = iterator.next();
                Map val = claims.get(key, Map.class);
                while (iterator.hasNext()) {
                    key = iterator.next();
                    if (iterator.hasNext()) {
                        val = (Map) val.get(key);
                    }
                }
                fieldValue = (String) val.get(key);
            }
            if (StringUtils.isBlank(fieldValue)) throw new IllegalArgumentException();
            return fieldValue;
        } catch (Exception e) {
            throw new TokenFormatNotValidException(String.format("Cannot extract value from field %s. The field does not exists, is empty, or is na object.", String.join(".", pathToField)));
        }
    }

}
