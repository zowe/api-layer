/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.service.internal;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class ZaasHttpClientProvider implements CloseableClientProvider {
    private final CloseableHttpClient httpClient;

    public ZaasHttpClientProvider() {
        httpClient = HttpClientBuilder.create().disableCookieManagement().disableAuthCaching().build();
    }

    @Override
    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }
}
