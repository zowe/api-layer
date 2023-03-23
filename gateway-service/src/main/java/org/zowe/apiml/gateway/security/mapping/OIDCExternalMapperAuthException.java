/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.mapping;

import lombok.experimental.Delegate;
import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.gateway.security.mapping.model.MapperResponse;

public class OIDCExternalMapperAuthException extends AuthenticationException {

    private static final long serialVersionUID = -457112306751545949L;

    @Delegate
    private final transient MapperResponse mapperResponse;

    public OIDCExternalMapperAuthException(String msg, MapperResponse mapperResponse) {
        super(msg);
        this.mapperResponse = mapperResponse;
    }

}
