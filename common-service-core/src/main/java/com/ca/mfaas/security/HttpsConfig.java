/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Builder
@Value
@ToString(exclude = { "trustStorePassword", "keyStorePassword", "keyPassword" })
public class HttpsConfig {
    @Builder.Default private String protocol = "TLSv1.2";
    @Builder.Default private String trustStore = null;
    @Builder.Default private String trustStorePassword = null;
    @Builder.Default private String trustStoreType = "PKCS12";
    @Builder.Default private boolean trustStoreRequired = false;
    @Builder.Default private String keyAlias = null;
    @Builder.Default private String keyStore = null;
    @Builder.Default private String keyStorePassword = null;
    @Builder.Default private String keyPassword = null;
    @Builder.Default private String keyStoreType = "PKCS12";
    @Builder.Default private boolean clientAuth = false;
    @Builder.Default private boolean verifySslCertificatesOfServices = true;
    @Builder.Default private String jwtSignatureAlgorithm = null;
    @Builder.Default private String jwtSecretType = null;
}
