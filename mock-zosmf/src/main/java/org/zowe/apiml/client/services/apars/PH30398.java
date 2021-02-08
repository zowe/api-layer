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

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PH30398 extends FunctionalApar {

    public PH30398(List<String> usernames, List<String> passwords) {
        super(usernames, passwords);
    }

    @Override
    protected Optional<ResponseEntity<?>> handleAuthenticationDefault(Map<String, String> headers) {
        String authorization = headers.get("authorization");
        if (containsInvalidUser(authorization) && noLtpaCookie(headers)) {
            return Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }

        return Optional.empty();
    }
}
