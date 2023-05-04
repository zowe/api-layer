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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.zowe.apiml.zss.model.MapperResponse;
import org.zowe.apiml.zss.model.OIDCRequest;
import org.zowe.apiml.zss.model.ZssResponse;

import java.util.Map;

@Service
@ConfigurationProperties(prefix = "zss")
@Setter
public class OIDCProvider {

    private Map<String, String> userMapping;

    public MapperResponse mapUserIdentity(OIDCRequest oidcRequest) {
        String username = userMapping.get(oidcRequest.getDn());
        if (username == null) {
            return new MapperResponse("", 8, 8, 8, 48);
        }
        return new MapperResponse(username, 0, 0, 0, 0);
    }

    public MapperResponse setCustomResponse(ZssResponse.ZssError zssError) {
        switch (zssError) {
            case MAPPING_NOT_AUTHORIZED:
                return new MapperResponse("", 8, 8, 8, 20);
            case MAPPING_EMPTY_INPUT:
                return new MapperResponse("", 8, 8, 8, 44);
            case MAPPING_OTHER:
                return new MapperResponse("", 4, 4, 0, 0);
            default:
                return null;
        }
    }

    public HttpStatus setCustomStatus(int statusCode) {
        return HttpStatus.valueOf(statusCode);
    }
}
