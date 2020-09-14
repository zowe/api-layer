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
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;

interface CloseableClientProvider {
    /**
     * Returns HTTP client provider with proper configuration fot HTTPS if HTTPS is used.
     *
     * @return Configured CloseableHttpClient that can be used for connections to the API ML ZAAS.
     * @throws ZaasConfigurationException Wrapper for errors in HTTP client and TLS configuration.
     */
    CloseableHttpClient getHttpClient() throws ZaasConfigurationException;
}
