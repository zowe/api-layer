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

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@SuppressWarnings("squid:S106")
public class HttpClientWrapper {

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    private final SSLContext sslContext;
    private final boolean useHttps;

    public HttpClientWrapper(SSLContext sslContext) {
        this.sslContext = sslContext;
        this.useHttps = true;
    }

    public HttpClientWrapper() {
        this.sslContext = null;
        this.useHttps = false;
    }

    public int executeCall(URL url, Map<String, String> headers) throws IOException {
        HttpURLConnection con;
        if (useHttps) {
            HttpsURLConnection httpsCon = (HttpsURLConnection) url.openConnection();
            httpsCon.setSSLSocketFactory(sslContext.getSocketFactory());
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
            return con.getResponseCode();
        } finally {
            con.disconnect();
        }
    }
}
