/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.ws;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.net.ssl.HostnameVerifier;

@Service
public class ServletSslContextFactoryProvider implements SslContextFactoryProvider {
    private final SslContextFactory.Server sslContextFactory;

    @Autowired
    public ServletSslContextFactoryProvider(SslContextFactory.Server sslContextFactory, HostnameVerifier secureHostnameVerifier) {
        sslContextFactory.setHostnameVerifier(secureHostnameVerifier);
        this.sslContextFactory = sslContextFactory;
    }

    @Override
    public SslContextFactory.Server getSslFactory() {
        return sslContextFactory;
    }
}
