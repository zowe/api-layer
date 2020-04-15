/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.zosmf;

import java.security.PublicKey;
import java.text.ParseException;
import java.util.Base64;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;

public class JwkToPublicKeyConverter {

    /**
     * Converts the first public key in JWT in JSON to PEM format.
     */
    public String convertFirstPublicKeyJwkToPem(String jwkJson) {
        try {
            PublicKey key = JWKSet.parse(jwkJson).toPublicJWKSet().getKeys().get(0).toRSAKey().toPublicKey();
            String encoded = Base64.getEncoder().encodeToString(key.getEncoded());
            StringBuilder s = new StringBuilder();
            s.append("-----BEGIN PUBLIC KEY-----");
            for (int i = 0; i < encoded.length(); i++) {
                if (((i % 64) == 0) && (i != (encoded.length() - 1))) {
                    s.append("\n");
                }
                s.append(encoded.charAt(i));
            }
            s.append("\n");
            s.append("-----END PUBLIC KEY-----\n");
            return s.toString();
        } catch (ParseException | JOSEException e) {
            throw new JwkConversionError(e);
        }
    }
}
