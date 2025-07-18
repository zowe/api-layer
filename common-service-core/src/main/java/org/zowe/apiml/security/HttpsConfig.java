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

import java.security.cert.X509Certificate;

@Builder
@Value
@ToString(exclude = {"trustStorePassword", "keyStorePassword", "keyPassword"})
public class HttpsConfig {

    @Builder.Default
    String protocol = "TLSv1.2";
    @Builder.Default
    String[] enabledProtocols = "TLSv1.2,TLSv1.3".split(",");
    String trustStore;
    char[] trustStorePassword;
    @Builder.Default
    String trustStoreType = "PKCS12";
    boolean trustStoreRequired;
    String keyAlias;
    String keyStore;
    char[] keyStorePassword;
    char[] keyPassword;
    @Builder.Default
    String[] cipherSuite = "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384".split(",");
    @Builder.Default
    String keyStoreType = "PKCS12";
    String clientAuth;
    @Builder.Default
    boolean verifySslCertificatesOfServices = true;
    @Builder.Default
    boolean nonStrictVerifySslCertificatesOfServices = false;
    @Builder.Default
    int maxConnectionsPerRoute = 10;
    @Builder.Default
    int maxTotalConnections = 100;
    @Builder.Default
    int idleConnTimeoutSeconds = 5;
    @Builder.Default
    int requestConnectionTimeout = 10_000;
    @Builder.Default
    int timeToLive = 10_000;
    X509Certificate certificate;
}
