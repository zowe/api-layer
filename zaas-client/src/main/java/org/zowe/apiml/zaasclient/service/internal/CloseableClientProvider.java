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
     * Return Closeable Client with properly set up SSL using Trust store.
     *
     * @return Valid closeable client to be used.
     * @throws ZaasConfigurationException Wrapper for errors in the crypto methods and IO based on the configuration.
     */
    CloseableHttpClient getHttpsClientWithTrustStore() throws ZaasConfigurationException;

    /**
     * Return Closeable Client with properly set up SSL using Trust store and properly set up private keys for
     * authentication via certificate.
     *
     * @return Valid closeable client to be used.
     * @throws ZaasConfigurationException Wrapper for errors in the crypto methods and IO based on the configuration.
     */
    CloseableHttpClient getHttpsClientWithKeyStoreAndTrustStore() throws ZaasConfigurationException;
}
