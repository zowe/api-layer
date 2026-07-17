/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.config;

import org.apache.commons.lang3.Strings;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import javax.servlet.http.HttpServletRequest;

public class ApimlStrictServerWebExchangeFirewall extends StrictHttpFirewall {

    private static final String[] BASE_PATH = {
        "/gateway",
        "/application",
        "/images",
        "/api-doc"
    };

    private StrictHttpFirewall nonRoutingFirewall = new StrictHttpFirewall();

    boolean isPathToRoute(String path, String[] prefixes) {
        // homepage
        if (Strings.CS.equals(path, "/")) {
            return false;
        }
        for (String prefix : prefixes) {
            if (Strings.CS.equals(path, prefix)) {
                return false;
            }
            if (Strings.CS.startsWith(path, prefix + "/")) {
                return false;
            }
        }
        return true;
    }

    boolean isPathToRoute(HttpServletRequest request) {
        return isPathToRoute(request.getRequestURI(), BASE_PATH);
    }

    @Override
    public FirewalledRequest getFirewalledRequest(HttpServletRequest request) throws RequestRejectedException {
        // in case of Gateway and a request to routing use a configured values
        if (isPathToRoute(request)) {
            return super.getFirewalledRequest(request);
        }

        return nonRoutingFirewall.getFirewalledRequest(request);
    }

}
