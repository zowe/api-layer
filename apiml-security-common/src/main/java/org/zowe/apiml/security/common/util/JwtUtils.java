/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.util;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.ExpiredJWTException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenFormatNotValidException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
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
     * @return parsed claims
     * @throws TokenNotValidException in case of invalid input, or TokenExpireException if JWT is expired
     */
    public JWTClaimsSet getJwtClaims(String jwt) {
        /*
         * Removes signature, because we don't have key to verify z/OS tokens, and we just need to read claim.
         * Verification is done by SAF itself. JWT library doesn't parse signed key without verification.
         */
        try {
            String jwtWithoutSignature = removeJwtSign(jwt);
            var token = JWTParser.parse(jwtWithoutSignature);
            var claims = token.getJWTClaimsSet();

            if (claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                throw new ExpiredJWTException("JWT token is expired");
            }
            return token.getJWTClaimsSet();
        } catch (RuntimeException | ParseException | BadJWTException exception) {
            throw handleJwtParserException(exception);
        }
    }

    /**
     * This method removes the token signature and replace algorithm with none. It allows to parse payload without
     * public key.
     *
     * @param jwtToken token to modify
     * @return unsigned jwt token
     * @throws BadJWTException
     */
    public String removeJwtSign(String jwtToken) throws BadJWTException {
        if (jwtToken == null) return null;

        int firstDot = jwtToken.indexOf('.');
        int lastDot = jwtToken.lastIndexOf('.');
        if ((firstDot < 0) || (firstDot >= lastDot)) {
            throw new BadJWTException("Invalid JWT format");
        }

        return HEADER_NONE_SIGNATURE + jwtToken.substring(firstDot, lastDot + 1);
    }

    /**
     * Method to translate original exception to internal one. It is used in case of parsing and verifying of JWT tokens.
     *
     * @param exception original exception
     * @return translated exception (better messaging and allow subsequent handling)
     */
    public RuntimeException handleJwtParserException(Exception exception) {
        if (exception instanceof ExpiredJWTException) {
            log.debug("Token is expired.");
            return new TokenExpireException("Token is expired.", exception);
        }
        if (exception instanceof BadJWTException || exception instanceof ParseException) {
            log.debug(TOKEN_IS_NOT_VALID_DUE_TO, exception.getMessage());
            return new TokenNotValidException("Token is not valid.", exception);
        }

        log.debug(TOKEN_IS_NOT_VALID_DUE_TO, exception.getMessage());
        return new TokenNotValidException("An internal error occurred while validating the token therefore the token is no longer valid.", exception);
    }

    boolean verifyJwtSignatureWithJwk() {
        return false;
    }

    /**
     * Extracts value of a field from an OIDC token. The value is extracted from a custom path which supports nested objects.
     * @param token to extract the field from
     * @param pathToField list of strings representing path to the field
     * @return list of values extracted from the token field
     *
     * @throws TokenFormatNotValidException in case of the field value cannot be extracted from the token, is null, or empty
     */
    public List<String> getFieldValuesFromToken(String token, List<String> pathToField) throws TokenFormatNotValidException {
        if (token == null || pathToField == null || pathToField.isEmpty() || StringUtils.isBlank(pathToField.get(0))) {
            throw new IllegalArgumentException("Token and field path must not be null or empty");
        }

        try {
            var claims = getJwtClaims(token);
            List<String> fieldValues;
            if (pathToField.size() == 1) {
                fieldValues = extractHighLevelField(claims, pathToField);
            } else {
                fieldValues = extractNestedFields(claims, pathToField);
            }

            fieldValues = fieldValues.stream().filter(StringUtils::isNotBlank).toList();
            if (fieldValues.isEmpty()) {
                throw new IllegalArgumentException();
            } else {
                return fieldValues;
            }
        } catch (Exception e) {
            throw new TokenFormatNotValidException(
                String.format("Cannot extract value from field %s. The field does not exist, is empty, or is an object.", String.join(".", pathToField)));
        }
    }

    private List<String> extractHighLevelField(JWTClaimsSet claims, List<String> pathToField) {
        return extractValueAsList(claims.getClaim(pathToField.get(0)));
    }

    @SuppressWarnings({ "rawtypes" })
    private List<String> extractNestedFields(JWTClaimsSet claims, List<String> pathToField) {
        var iterator = pathToField.iterator();
        var key = iterator.next();

        var claim = claims.getClaim(key);
        while (iterator.hasNext()) {
            key = iterator.next();
            if (iterator.hasNext()) {
                if (claim instanceof Map val) {
                    claim = val.get(key);
                }
            }
        }

        return extractValueAsList(((Map) claim).get(key));
    }

    @SuppressWarnings("unchecked")
    private List<String> extractValueAsList(Object rawValue) {
        if (rawValue instanceof String value) {
            return List.of(value);
        } else if (rawValue instanceof List values) {
            return values;
        } else {
            throw new IllegalArgumentException("Field value is neither String nor List of Strings");
        }

    }

}
