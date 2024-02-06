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

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Factory for creating a new {@link CloseableHttpClient}.
 *
 * @author Ryan Baxter
 */
public interface ApacheHttpClientFactory {

    /**
     * Creates an {@link HttpClientBuilder} that can be used to create a new
     * {@link CloseableHttpClient}.
     * @return A {@link HttpClientBuilder}.
     */
    HttpClientBuilder createBuilder();

}
