/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TlsConfiguration {
    private String keyAlias;
    private char[] keyPassword;
    private String keyStoreType;
    private String keyStore;
    private String clientKeystore;
    private char[] keyStorePassword;
    private String trustStoreType;
    private String trustStore;
    private char[] trustStorePassword;
}
