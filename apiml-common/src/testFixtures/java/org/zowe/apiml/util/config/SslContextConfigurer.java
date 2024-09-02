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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

@Getter
@EqualsAndHashCode
public class SslContextConfigurer {

    private final char[] keystorePassword;
    private final String keystoreLocalhostJks;
    private final String keystore;
    private final X509HostnameVerifier hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

    public SslContextConfigurer(char[] keystorePassword, String keystore_client_cert, String keystore) {
        this.keystorePassword = keystorePassword;
        this.keystoreLocalhostJks = keystore_client_cert;
        this.keystore = keystore;
    }

}
