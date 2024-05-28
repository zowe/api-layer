/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema;

import java.util.Optional;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;


/**
 * This is abstract class for any processor which support service's authentication. They are called from ZUUL filters
 * to decorate request for target services.
 *
 * For each type of scheme should exist right one implementation.
 */
public interface IAuthenticationScheme {

    /**
     * @return Scheme which is supported by this component
     */
    AuthenticationScheme getScheme();

    /**
     * This method decorate the request for target service
     *
     * @param authentication DTO describing details about authentication
     * @param authSource User's authentication source (Zowe's JWT token, client certificate, etc.)
     */
    AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource);

    /**
     * Returns authentication source according to the logic of the scheme. By default, authentication source is empty.
     * @return Optional of user's authentication source (Zowe's JWT token, client certificate, etc.) or empty Optional.
     */
    default Optional<AuthSource> getAuthSource() {
        return Optional.empty();
    }

    /**
     * Define implementation, which will be use in case no scheme is defined.
     *
     * @return true if this implementation is default, otherwise false
     */
    default boolean isDefault() {
        return false;
    }

}
