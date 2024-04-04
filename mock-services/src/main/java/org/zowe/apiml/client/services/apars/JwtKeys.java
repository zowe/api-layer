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

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.client.services.JwtTokenService;

import java.util.List;

public class JwtKeys extends FunctionalApar {
    public JwtKeys(List<String> usernames, List<String> passwords) {
        super(usernames, passwords);
    }

    @Override
    protected ResponseEntity<?> handleJwtKeys() {
        try {
            JWKSet jwkSet = JwtTokenService.getKeySet();
            return new ResponseEntity<>(jwkSet.toJSONObject(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
