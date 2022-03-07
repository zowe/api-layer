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

import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;

/**
 * This command extends {@link AuthenticationCommand} and keeps common validation logic for authentication source. */
public abstract class AuthenticationCommandWithValidation extends AuthenticationCommand {

    public abstract AuthSourceService getAuthSourceService();

    @Override
    public boolean isRequiredValidSource() {
        return true;
    }

    @Override
    public boolean isValidSource(AuthSource authSource) {
        return getAuthSourceService().isValid(authSource);
    }
}
