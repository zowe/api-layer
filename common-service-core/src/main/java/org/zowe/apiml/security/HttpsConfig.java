/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Builder
@Value
@ToString(exclude = {"trustStorePassword", "keyStorePassword", "keyPassword"})
public class HttpsConfig {

    @Builder.Default
    private String protocol = "TLSv1.2";
    private String trustStore;
    private char[] trustStorePassword;
    @Builder.Default
    private String trustStoreType = "PKCS12";
    private boolean trustStoreRequired;
    private String keyAlias;
    private String keyStore;
    private char[] keyStorePassword;
    private char[] keyPassword;
    @Builder.Default
    private String keyStoreType = "PKCS12";
    private boolean clientAuth;
    @Builder.Default
    private boolean verifySslCertificatesOfServices = true;
    @Builder.Default
    private boolean nonStrictVerifySslCertificatesOfServices = false;
    @Builder.Default
    private int maxConnectionsPerRoute = 10;
    @Builder.Default
    private int maxTotalConnections = 100;
    @Builder.Default
    private int idleConnTimeoutSeconds = 5;
    @Builder.Default
    private int requestConnectionTimeout = 10_000;
    @Builder.Default
    private int timeToLive = 10_000;
}
