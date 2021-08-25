/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.saf;

import java.util.Optional;

/**
 * It's possible to configure various SafIdtProviders. At the moment only one configured at the time is allowed. If there
 * are multiple providers configures, the behavior could be unpredictable.
 */
public interface SafIdtProvider {
    /**
     * If the current user has the proper rights generate the SAF token on its behalf and return it back.
     *
     * @return Either empty answer meaning the user is either unauthenticated or doesn't have the proper rights.
     */
    Optional<String> generate(String username);

    /**
     * Verify that the provided saf token is valid.
     *
     * @param safToken Token to validate.
     * @return true if the token is valid, false if it is invalid
     */
    boolean verify(String safToken);
}
