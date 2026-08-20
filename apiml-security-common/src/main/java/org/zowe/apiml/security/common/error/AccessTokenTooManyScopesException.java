/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.error;

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

import java.io.Serial;

/**
 * Thrown when a personal access token is requested with more scopes than validating it could ever look up.
 * <p>
 * The cap exists at issuance rather than only at validation because the validation-side limit fails closed:
 * a token issued above it would stop authenticating on its very next request, with no recovery short of
 * issuing a new one.
 */
@Getter
public class AccessTokenTooManyScopesException extends AuthenticationException {

    @Serial
    private static final long serialVersionUID = -6072673487351985199L;

    private final int limit;

    public AccessTokenTooManyScopesException(String message, int limit) {
        super(message);
        this.limit = limit;
    }
}
