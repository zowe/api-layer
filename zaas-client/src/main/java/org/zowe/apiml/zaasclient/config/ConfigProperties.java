/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigProperties {

    private String apimlHost;
    private String apimlPort;
    private String apimlBaseUrl;
    private String keyStoreType;
    private String keyStorePath;
    private String keyStorePassword;
    private String trustStoreType;
    private String trustStorePath;
    private String trustStorePassword;

    @Override
    public String toString() {
        return "ConfigProperties{" +
            "apimlHost='" + apimlHost + '\'' +
            ", apimlPort='" + apimlPort + '\'' +
            ", apimlBaseUrl='" + apimlBaseUrl + '\'' +
            ", keyStoreType='" + keyStoreType + '\'' +
            ", keyStorePath='" + keyStorePath + '\'' +
            ", keyStorePassword='" + "" + '\'' +
            ", trustStoreType='" + trustStoreType + '\'' +
            ", trustStorePath='" + trustStorePath + '\'' +
            ", trustStorePassword='" + "" + '\'' +
            '}';
    }
}
