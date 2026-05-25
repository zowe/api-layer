/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP/HTTPS client wrapping {@link java.net.HttpURLConnection}.
 * Supports custom {@link SSLContext} and {@link HostnameVerifier} for
 * STRICT, NONSTRICT, and DISABLED certificate verification modes.
 */
public class HttpClientWrapper {

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    public static class Response {
        private final int statusCode;
        private final String body;

        public Response(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public int getStatusCode() { return statusCode; }
        public String getBody() { return body; }
    }

    private final SSLContext sslContext;
    private final boolean useHttps;
    private final HostnameVerifier hostnameVerifier;

    public HttpClientWrapper(SSLContext sslContext, HostnameVerifier hostnameVerifier) {
        this.sslContext = sslContext;
        this.useHttps = true;
        this.hostnameVerifier = hostnameVerifier;
    }

    public HttpClientWrapper() {
        this.sslContext = null;
        this.useHttps = false;
        this.hostnameVerifier = null;
    }

    public Response executeCall(URL url, Map<String, String> headers) throws IOException {
        HttpURLConnection con;
        if (useHttps) {
            HttpsURLConnection httpsCon = (HttpsURLConnection) url.openConnection();
            httpsCon.setSSLSocketFactory(sslContext.getSocketFactory());
            if (hostnameVerifier != null) {
                httpsCon.setHostnameVerifier(hostnameVerifier);
            }
            con = httpsCon;
        } else {
            con = (HttpURLConnection) url.openConnection();
        }

        con.setRequestMethod("GET");
        con.setConnectTimeout(CONNECT_TIMEOUT);
        con.setReadTimeout(READ_TIMEOUT);

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                con.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        try {
            int responseCode = con.getResponseCode();
            String body = readBody(con);
            return new Response(responseCode, body);
        } finally {
            con.disconnect();
        }
    }

    private String readBody(HttpURLConnection con) {
        try {
            InputStream is = con.getErrorStream() != null ? con.getErrorStream() : con.getInputStream();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            return null;
        }
    }
}
