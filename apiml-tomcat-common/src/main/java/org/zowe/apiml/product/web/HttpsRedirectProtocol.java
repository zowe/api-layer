/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.web;

import org.apache.coyote.http11.Http11NioProtocol;

/**
 * Keeps Tomcat's TLS implementation, but redirects plaintext HTTP on the TLS listener.
 */
public class HttpsRedirectProtocol extends Http11NioProtocol {

    public HttpsRedirectProtocol() {
        super(new HttpsRedirectEndpoint());
    }

    public void setHttpsRedirectOrigin(String origin) {
        ((HttpsRedirectEndpoint) getEndpoint()).setRedirectOrigin(origin);
    }

    public void setHttpsRedirectHostname(String hostname) {
        ((HttpsRedirectEndpoint) getEndpoint()).setRedirectHostname(hostname);
    }

    @Override
    public void init() throws Exception {
        ((HttpsRedirectEndpoint) getEndpoint()).configureRequestParsing(
            getMaxHttpRequestHeaderSize(), getRelaxedPathChars(), getRelaxedQueryChars());
        super.init();
    }
}
