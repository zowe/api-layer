/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.services;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class JwtTokenService {

    public static final String JWT_TOKEN = "jwtToken=";
    public static final String LTPA_TOKEN = "LtpaToken2=";
    private Set<String> invalidatedTokens = new HashSet<>();

    private int expirationSeconds;

    public JwtTokenService(int expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }

    public String generateJwt(String user) throws NoSuchAlgorithmException, InvalidKeySpecException {

        return Jwts.builder()
            .setHeaderParam("kid", "ozG_ySMHRsVQFmN1mVBeS-WtCupY1r-K7ewben09IBg")
            .setHeaderParam("typ", "JWT")
            .setHeaderParam("alg", "RS256")
            .signWith(readPemPrivateKey())
            .setSubject(user)
            .setIssuer("zOSMF")
            .setIssuedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
            .setExpiration(Date.from(LocalDateTime.now().plusSeconds(expirationSeconds).atZone(ZoneId.systemDefault()).toInstant()))
            .compact();
    }

    public boolean validateJwtToken(String token) {
        if (invalidatedTokens.contains(token)) {
            return false;
        }
        try {
            JwtParser parser = Jwts.parser()
                .verifyWith(readPemPublicKey())
                .build();
            parser.parseClaimsJws(token).getPayload();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void invalidateJwtToken(String token) {
        invalidatedTokens.add(token);
    }

    public boolean containsToken(String token) {
        return invalidatedTokens.contains(token);
    }


    public static JWKSet getKeySet() throws NoSuchAlgorithmException, InvalidKeySpecException {
        ArrayList<JWK> keys = new ArrayList<>();

        keys.add(loadJWK(readAnotherPemPublicKey(), "someotherkey"));
        keys.add(loadJWK(readPemPublicKey(), "ozG_ySMHRsVQFmN1mVBeS-WtCupY1r-K7ewben09IBg"));

        return new JWKSet(keys);
    }

    private static JWK loadJWK(RSAPublicKey publicKey, String kid) {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .keyID(kid)
            .build();
        return rsaKey.toPublicJWK();
    }


    public static RSAPrivateKey readPemPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String key = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCOkSanuY2fHxuL\n" +
            "+cXaWosSQ3VzFSfWotq9TYQDLvReZf2tlLHWGUDWTxK3VqkPrvygj45vfwxpv69O\n" +
            "ueT2e8mCzp7rua2ybTQ4/WakYfNBJjZYPADk4Yu/70V4MjodOEAfVwTPXjylEG2I\n" +
            "31WRUz47NXb6+ofmFc7a+dKd5SeciUxVnX4nsblYg8ksPGh1dYWqd7yXnpJghFbP\n" +
            "ratMrQfB7SCEAyuyJPPR5UqB9Wvvqs/SEhhkzALdvVvF+GPBQ65DGMR5gyHaMQv7\n" +
            "k2/YhwR87NdgQ4L8isP+stribid8Gz4kmLDiE6Ae+PN03P0TNXQP4dJasicdcR3+\n" +
            "BaMOfiwXAgMBAAECggEAMU1xGL/KgiS32gheq8x0G7TIgSvnwwo+qwiLhq5OQ/bx\n" +
            "a33ooinJilN+HXkSriHNq5j5oQVGvatUbN1MmRDl9x6NRufHcdTiInM/c8mL3hPg\n" +
            "51KY3I5DTfTpCVAVWNWDF1N4jl4AivTLbHIPnVo0QzWSF+lb5e3Uw1VxyLjeofs0\n" +
            "RFNQacxhAE8TK1kAx5HXpmbB20MuX+rkvKXKoQ/ppaj4gWifdl1pKz7xPL6okcYz\n" +
            "goXIShdtAL4IDOvyE7A8jhjBH9Bf3Vftn1umzUxvTUrAZgSI7FYGIUEKBcw+3ygG\n" +
            "bLy5j8tSsWJTeLykVEc+ZwjZee6VCUMFNn+9my4QQQKBgQDVNYJYDazUiTg8Sqh7\n" +
            "941etnimZOjYFfbcBww2qid6Rw6MxvAGo5fOnBqqbgvttFDw6mKkPzvEbjx0fySS\n" +
            "S+ZfMB1Nqd5xhSxgm2Jrsr1wT/9HsPbOi316E4EDePyy9bpt9NSI5vwcteHU8Wpt\n" +
            "mkaQzjXm+/+OXoyDru9p6veqewKBgQCrLh38qEmg++8RsDEzPXKwNm6AiH+U2H/f\n" +
            "XRHJI0LVb9DFrsjbJp+VtJIBzzyobT7h+B3vw/lY0eAMHJUeACMFiXq0bsGy+nnt\n" +
            "h6p8UgdtB1BDrijXrG7DYCJxUG6Z5aJDhu53LbsLFVthE6qedlzUdNNnC9Vl9o5p\n" +
            "xDt/OliQFQKBgQDIOn9ViEo2M0Pfw1FlUn+uYfj+cygEvuPdkLTUpYl7mT290Zpa\n" +
            "8cnAW/Pi+IQ1UTDuf3/xtfzAJbKayUikJ6mK3Vm3tP7VZ3bcpzCP6gVkc4xPXI78\n" +
            "PB2zxptTkozm2ESjvNjYVOyRXfJfE/WaRtdcaHxQl3pRztNxW5k1xFeg/wKBgFu9\n" +
            "bH7C5irrujVdmxCeBwAfO9uQy+dGnElmBKkqR6BBu76mLKkeqvo9et6TZSvS2Jec\n" +
            "NNcRzWlnmU6EZvpcEmjeRC+9B/xWts+xHJJiF+67s62CAguMMxRsSik2dP/vjKXq\n" +
            "A5VFoe+Ps5h0RMWGI7wNHFsmgWiS2cIfU8+cwmf9AoGAR3vutUBt0+pMqPlAREPQ\n" +
            "BHUjZXhYrxHoD2kvxFkDVlMvPP9GVy6lgjEt1S28oRNjiES2AZWoSoqEbe0ZHg7t\n" +
            "OfsydqmJqfwJaLoAkRzxdqJ66KH2m/BEOBapxMr8B79hfcjpMf2O+T76+6hMFF6j\n" +
            "RIpBLV1t4pDsd7fvwxpR3vA=\n" +
            "-----END PRIVATE KEY-----\n";

        String privateKeyPEM = key
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replaceAll("\n", "") //NOSONAR
            .replace("-----END PRIVATE KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    public static RSAPublicKey readPemPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String key = "-----BEGIN PUBLIC KEY-----\n" + //NOSONAR
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjpEmp7mNnx8bi/nF2lqL\n" +
            "EkN1cxUn1qLavU2EAy70XmX9rZSx1hlA1k8St1apD678oI+Ob38Mab+vTrnk9nvJ\n" +
            "gs6e67mtsm00OP1mpGHzQSY2WDwA5OGLv+9FeDI6HThAH1cEz148pRBtiN9VkVM+\n" +
            "OzV2+vqH5hXO2vnSneUnnIlMVZ1+J7G5WIPJLDxodXWFqne8l56SYIRWz62rTK0H\n" +
            "we0ghAMrsiTz0eVKgfVr76rP0hIYZMwC3b1bxfhjwUOuQxjEeYMh2jEL+5Nv2IcE\n" +
            "fOzXYEOC/IrD/rLa4m4nfBs+JJiw4hOgHvjzdNz9EzV0D+HSWrInHXEd/gWjDn4s\n" +
            "FwIDAQAB\n" +
            "-----END PUBLIC KEY-----"; //NOSONAR

        return getRsaPublicKey(key);
    }

    public static RSAPublicKey readAnotherPemPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String key = "-----BEGIN PUBLIC KEY-----\n" + //NOSONAR
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDCFENGw33yGihy92pDjZQhl0C3\n" +
            "6rPJj+CvfSC8+q28hxA161QFNUd13wuCTUcq0Qd2qsBe/2hFyc2DCJJg0h1L78+6\n" +
            "Z4UMR7EOcpfdUE9Hf3m/hs+FUR45uBJeDK1HSFHD8bHKD6kv8FPGfJTotc+2xjJw\n" +
            "oYi+1hqp1fIekaxsyQIDAQAB\n" +
            "-----END PUBLIC KEY-----"; //NOSONAR

        return getRsaPublicKey(key);
    }

    private static RSAPublicKey getRsaPublicKey(String pemKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String publicKeyPEM = pemKey
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replaceAll("\n", "") //NOSONAR
            .replace("-----END PUBLIC KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    public String extractToken(Map<String, String> headers) {
        if (headers == null) {
            return "";
        }
        return getTokenFromTheStart(headers)
            .orElse(getTokenFromTheMiddle(headers)
                .orElse(""));
    }

    public String extractLtpaToken(Map<String, String> headers) {
        if (headers == null) {
            return "";
        }
        return getLtpaToken(headers)
            .orElse("");
    }

    private Optional<String> getTokenFromTheStart(Map<String, String> headers) {
        return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("cookie") && e.getValue().startsWith(JWT_TOKEN))
            .map(Map.Entry::getValue).map(s -> s.replaceFirst(JWT_TOKEN, "")).findFirst();
    }

    private Optional<String> getTokenFromTheMiddle(Map<String, String> headers) {
        return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("cookie") && e.getValue().startsWith(LTPA_TOKEN))
            .map(Map.Entry::getValue).map(s -> s.substring(s.indexOf(JWT_TOKEN) + JWT_TOKEN.length())).findFirst();
    }

    private Optional<String> getLtpaToken(Map<String, String> headers) {
        return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("cookie") && e.getValue().startsWith(LTPA_TOKEN))
            .map(Map.Entry::getValue).map(s -> s.substring(s.indexOf(LTPA_TOKEN) + LTPA_TOKEN.length())).findFirst();
    }
}
