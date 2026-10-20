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

import org.apache.tomcat.util.http.parser.HttpParser;
import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.net.SocketBufferHandler;

import java.net.URI;
import java.net.URISyntaxException;

class HttpsRedirectEndpoint extends NioEndpoint {

    private String redirectOrigin;
    private String redirectHostname = "localhost";
    private int requestLimit;
    private HttpParser parser;

    void setRedirectOrigin(String origin) {
        redirectOrigin = normalizeOrigin(origin);
    }

    static String normalizeOrigin(String origin) {
        URI uri = URI.create(origin);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
            || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
            || (!uri.getRawPath().isEmpty() && !"/".equals(uri.getRawPath()))
            || uri.getPort() == 0 || uri.getPort() > 65535) {
            throw new IllegalArgumentException("HTTP redirect destination must be an HTTPS origin");
        }
        return "https://" + uri.getRawAuthority();
    }

    void setRedirectHostname(String hostname) {
        origin(hostname, -1);
        redirectHostname = hostname;
        redirectOrigin = null;
    }

    static String origin(String hostname, int port) {
        try {
            return normalizeOrigin(new URI("https", null, hostname, port, null, null, null).toASCIIString());
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid HTTPS redirect hostname or port", e);
        }
    }

    void configureRequestParsing(int requestLimit, String relaxedPathChars, String relaxedQueryChars) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("HTTP redirect request limit must be positive");
        }
        this.requestLimit = requestLimit;
        parser = new HttpParser(relaxedPathChars, relaxedQueryChars);
    }

    @Override
    protected NioChannel createChannel(SocketBufferHandler buffer) {
        if (!isSSLEnabled()) {
            return super.createChannel(buffer);
        }
        String origin = redirectOrigin == null ? origin(redirectHostname, getLocalPort()) : redirectOrigin;
        return new HttpsRedirectChannel(buffer, this, origin, requestLimit, parser);
    }
}
