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

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import org.zowe.apiml.zss.model.MapperResponse;
import org.zowe.apiml.zss.model.OIDCRequest;

import java.util.Map;

@Service
@ConfigurationProperties(prefix = "zss")
@Setter
public class OIDCProvider {

    private Map<String, String> userMapping;

    public MapperResponse mapUserIdentity(OIDCRequest oidcRequest) {
        String username = userMapping.get(oidcRequest.getDn());
        if (username == null) {
            return new MapperResponse(null, 8, 8, 8, 48);
        }
        return new MapperResponse(username, 0, 0, 0, 0);
    }
}
