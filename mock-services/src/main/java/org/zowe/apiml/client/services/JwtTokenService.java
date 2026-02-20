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
            .claim("uuid", UUID.randomUUID())
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
        String key = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDIeZZvb+SHW2Yv
            PkW/FhiKDCLCpWb0fIfCzIzl+CvLwbQs8L9rtbALJcvdFcm7so4l0rkl75b+tVnM
            z3Kv4EQQIHT5Dd6GVwAKphu38UAJunZUhDfJcd+6nTxNxCWCaPPESLvzevKpdk7v
            KQTJ6pcvJMqPcRtLV8U4Rt9JxbmzqQ+a3r+weY7LfNPhylsmydHa+aW7A3cNe7WY
            /dy4UyHuh5V3oT8+4CMbXl8JAHzMI2tlItVb4zJ4oUKTFwr670S22opXGE0F64/k
            SrJ0VqS1Sd/ytbTxtpJECaMtW6r4S/GVHJNb2ciDk9b2/O3y3URpQqknGHAFy/lD
            p3ZJdb1LAgMBAAECggEAUm07n0IAUm8QCqiuAK1TFZ97w1BCjo+NOljkLcHmL/bK
            4Bd0fRLM+ShnM9W7hkMaEw8bNS/Xg46JB57b5ths47yUm7pKHi4zRC2cA/tbeySB
            dqqOTXNWq+HuY4MccQw5/iBxtuB8WZTdS5Qv9d1Qn/+ekW/N6yKBYmwxbqGvguBq
            kEqDoovihVZ5TMpXjKOTVkeIyQT6nfKZaJFxp4h8x189VoK3wTeSDGh+A0xd84WB
            gjRn+qRgggbDEZs2onVR6a93vdHMRGqmeA6f80aSahdwpgzNhyzwmLSkhanVpufA
            y8hR7wtANxPBam9G9wGwi2GgBGqkFgB3LazdU4wSYQKBgQD+Zirb1CC0Ay15bdUg
            daudoL4Og02Z+9ElEgRt64Zgy6RiG8Vaw0cAULkFjfi4g5ImfSW5N4i19HqQzgZq
            /GpkqSclrzI4zRO2sIK3JtxoVaWVjjNiWUg4+ZaLlc0Lr+hpCa0hp51PB0Pkmfau
            Zto5xlOhmKmMqJhx+NvtWHCHmQKBgQDJvIyigZ8+OM4kBa4Cqu6We5tYbuQ12gn0
            nP0FmfOvOD6Ry8YsAMhIxlPZs3qFS9waq3iwwfGvZZ+gCdOmCiXnpPROUUcY2zKb
            jnGCGLAWZ0M1J4VsfnPJBRWTpXs/anaTPyLHKE1HSCyGNUSO342LOY2A1tIRguTa
            cGXGdnNqgwKBgBIhuAZI/Te3TkNsV5djq6KldUZVh29lKkfpG9W1xrMZcJLphcxt
            RJ86IaXKs6J7BiymGM01rxHA5gdyF7UCXpbkE301GnA/9Zq8w+RH6cep6w5Yv0LQ
            ODyPVXKHb8DYfckWvnc6mhSq4OTnMFTH0d/ySb2nwtXaolrlMM+e0Q5pAoGARMhV
            xv6dFfD2UA/jsaMoMAS7BZ7hjn7mEBIKrwM7s81ggANTcSNfJnkAk+R+7L3dsPYv
            80xdJClpEH7pO96P5/g1GBLcQ9xQ1/rsNqhGOY1Bu/japBBFWA+0uJ+ecxPQlAnd
            yLu0BY7VJ3k4A+Ky9vpLhdc4zqGrd9ME0HMIjRMCgYEA3gXjXhM7//RrAsNWmdtH
            5gtXysgE3T4+teqE11R9aRFayXf8UIItR1ZQfxEC05CRbsSThv/C9q6aEUjQanmk
            y2X8v3Ghh1+mk2tLXPhuL5T81bzSJ1Ad3RhK9jOpC0n/1l+IiTjOFp5455eRYlmi
            wHvnCGrhCcOCm09SP+LeRkQ=
            -----END PRIVATE KEY-----
            """;

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
        String key = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyHmWb2/kh1tmLz5FvxYY
            igwiwqVm9HyHwsyM5fgry8G0LPC/a7WwCyXL3RXJu7KOJdK5Je+W/rVZzM9yr+BE
            ECB0+Q3ehlcACqYbt/FACbp2VIQ3yXHfup08TcQlgmjzxEi783ryqXZO7ykEyeqX
            LyTKj3EbS1fFOEbfScW5s6kPmt6/sHmOy3zT4cpbJsnR2vmluwN3DXu1mP3cuFMh
            7oeVd6E/PuAjG15fCQB8zCNrZSLVW+MyeKFCkxcK+u9EttqKVxhNBeuP5EqydFak
            tUnf8rW08baSRAmjLVuq+EvxlRyTW9nIg5PW9vzt8t1EaUKpJxhwBcv5Q6d2SXW9
            SwIDAQAB
            -----END PUBLIC KEY-----"""; //NOSONAR

        return getRsaPublicKey(key);
    }

    public static RSAPublicKey readAnotherPemPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String key = """
            -----BEGIN PUBLIC KEY-----
            MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDCFENGw33yGihy92pDjZQhl0C3
            6rPJj+CvfSC8+q28hxA161QFNUd13wuCTUcq0Qd2qsBe/2hFyc2DCJJg0h1L78+6
            Z4UMR7EOcpfdUE9Hf3m/hs+FUR45uBJeDK1HSFHD8bHKD6kv8FPGfJTotc+2xjJw
            oYi+1hqp1fIekaxsyQIDAQAB
            -----END PUBLIC KEY-----"""; //NOSONAR

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
