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

import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.net.SocketBufferHandler;

import java.net.URI;

class HttpsRedirectEndpoint extends NioEndpoint {

    private String redirectOrigin;
    private int requestLimit = 8192;

    void setRedirectOrigin(String origin) {
        URI uri = URI.create(origin);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
            || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
            || (!uri.getRawPath().isEmpty() && !"/".equals(uri.getRawPath()))
            || uri.getPort() == 0 || uri.getPort() > 65535) {
            throw new IllegalArgumentException("HTTP redirect destination must be an HTTPS origin: " + origin);
        }
        redirectOrigin = "https://" + uri.getRawAuthority();
    }

    void setRequestLimit(int requestLimit) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("HTTP redirect request limit must be positive");
        }
        this.requestLimit = requestLimit;
    }

    @Override
    protected NioChannel createChannel(SocketBufferHandler buffer) {
        if (!isSSLEnabled()) {
            return super.createChannel(buffer);
        }
        if (redirectOrigin == null) {
            throw new IllegalStateException("Missing HTTPS redirect origin");
        }
        return new HttpsRedirectChannel(buffer, this, redirectOrigin, requestLimit);
    }
}
