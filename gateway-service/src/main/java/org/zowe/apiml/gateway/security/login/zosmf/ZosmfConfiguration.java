/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.login.zosmf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZosmfConfiguration {

    public enum JWT_AUTOCONFIGURATION_MODE {
        AUTO,
        LTPA,
        JWT
    }

    @Value("${apiml.security.auth.zosmfJwtAutoconfiguration:AUTO}")
    public JWT_AUTOCONFIGURATION_MODE jwtAutoconfigurationMode;

    public static ZosmfConfiguration of(JWT_AUTOCONFIGURATION_MODE mode) {
        ZosmfConfiguration configuration = new ZosmfConfiguration();
        configuration.jwtAutoconfigurationMode = mode;
        return configuration;
    }

}
