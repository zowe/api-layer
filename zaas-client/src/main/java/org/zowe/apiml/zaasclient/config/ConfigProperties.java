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

import lombok.Data;

@Data
public class ConfigProperties {
    private String apimlHost;
    private String apimlPort;
    private String apimlBaseUrl;
    private String keyStoreType;
    private String keyStorePath;
    private char[] keyStorePassword;
    private String trustStoreType;
    private String trustStorePath;
    private char[] trustStorePassword;
    private boolean httpOnly;
}
