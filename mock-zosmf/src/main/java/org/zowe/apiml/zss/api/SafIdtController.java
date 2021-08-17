/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zss.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.zss.model.Authentication;
import org.zowe.apiml.zss.model.Token;
import org.zowe.apiml.zss.services.SafIdtProvider;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class SafIdtController {
    private final SafIdtProvider provider;

    /**
     * Create the token for the specific user as long as the username and jwt are provided.
     *
     * @param authentication Authentication information to verify that the user is properly authenticated and so the
     *                       token should be issued.
     * @return Appropriate response code
     *   - 401 - Not authenticated
     *   - 400 - The required information wasn't provided
     *   - 201 - Valid SAF IDT token.
     */
    @PostMapping(value="/zss/saf/authenticate")
    public ResponseEntity<?> authenticate(
        @RequestBody Authentication authentication
    ) {
        if (StringUtils.isEmpty(authentication.getJwt()) || StringUtils.isEmpty(authentication.getUsername())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Optional<Token> token = provider.authenticate(authentication);

        if (token.isPresent()) {
            return new ResponseEntity<>(token.get(), HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Validate provided token.
     *
     * @param token Token to verify
     * @return Appropriate response code
     *   - 401 - The token is invalid
     *   - 400 - The required information wasn't provided
     *   - 200 - The token is valid
     */
    @PostMapping(value="/zss/saf/verify")
    public ResponseEntity<?> verify(
        @RequestBody Token token
    ) {
        if (StringUtils.isEmpty(token.getJwt())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (provider.verify(token)) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }
}
