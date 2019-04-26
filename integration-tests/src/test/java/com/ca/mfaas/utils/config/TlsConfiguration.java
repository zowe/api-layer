/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.utils.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TlsConfiguration {
    private String keyAlias;
    private String keyPassword;
    private String keyStoreType;
    private String keyStore;
    private String keyStorePassword;
    private String trustStoreType;
    private String trustStore;
    private String trustStorePassword;
    private String protocol;
    private List<String> ciphers;
}
