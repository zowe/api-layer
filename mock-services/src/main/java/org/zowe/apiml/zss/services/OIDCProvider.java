/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zss.services;

import org.springframework.stereotype.Service;
import org.zowe.apiml.zss.model.MapperResponse;
import org.zowe.apiml.zss.model.OIDCRequest;

@Service
public class OIDCProvider {

    public MapperResponse mapUserIdentity(OIDCRequest oidcRequest) {
        return new MapperResponse("USER", 0, 0, 0, 0);
    }
}
