/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.schema;

import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.util.function.Supplier;

/**
 * This is abstract class for any processor which support service's authentication. They are called from ZUUL filters
 * to decorate request for target services.
 *
 * For each type of scheme should exist right one implementation.
 */
public interface AbstractAuthenticationScheme {

    /**
     * @return Scheme which is supported by this component
     */
    public AuthenticationScheme getScheme();

    /**
     * This method decorate the request for target service
     *
     * @param authentication DTO describing details about authentication
     * @param token User's parsed (Zowe's) JWT token, evaluated only, if needed
     */
    public AuthenticationCommand createCommand(Authentication authentication, Supplier<QueryResponse> token);

    /**
     * Define implementation, which will be use in case no scheme is defined.
     *
     * @return true if this implementation is default, otherwise false
     */
    public default boolean isDefault() {
        return false;
    }

}
