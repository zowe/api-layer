/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.oidc;

import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Objects;

@UtilityClass
public class OidcUtils {

    private String PREFIX = "ZWE_components_gateway_spring_security_oauth2_client_";

    public List<String> getOidcProvider() {
        return System.getenv().keySet().stream()
            .filter(Objects::nonNull)
            .filter(key -> key.startsWith(PREFIX))
            .map(key -> key.substring(PREFIX.length()))
            .map(key -> key.split("_"))
            .filter(parts -> parts.length > 2)
            .map(parts -> parts[1])
            .distinct()
            .sorted()
            .toList();
    }

}
