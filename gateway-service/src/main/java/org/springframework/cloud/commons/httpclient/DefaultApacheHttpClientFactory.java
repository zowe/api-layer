/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.springframework.cloud.commons.httpclient;

import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Default implementation of {@link ApacheHttpClientFactory}.
 *
 * @author Ryan Baxter
 */
public class DefaultApacheHttpClientFactory implements ApacheHttpClientFactory {

    private HttpClientBuilder builder;

    public DefaultApacheHttpClientFactory(HttpClientBuilder builder) {
        this.builder = builder;
    }

    /**
     * A default {@link HttpClientBuilder}. The {@link HttpClientBuilder} returned will
     * have content compression disabled, have cookie management disabled, and use system
     * properties.
     */
    @Override
    public HttpClientBuilder createBuilder() {
        return this.builder.disableContentCompression().disableCookieManagement()
                .useSystemProperties();
    }

}
