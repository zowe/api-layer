/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ssl {
    private boolean enabled;
    private boolean verifySslCertificatesOfServices;
    private String protocol;
    private String keyAlias;
    private String keyPassword;
    private String keyStore;
    private String keyStorePassword;
    private String keyStoreType;
    private String trustStore;
    private String trustStorePassword;
    private String trustStoreType;
}
