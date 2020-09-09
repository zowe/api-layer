/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.service;

import org.springframework.stereotype.Service;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.service.ZaasClient;
import org.zowe.apiml.zaasclient.service.internal.ZaasClientImpl;

import java.io.IOException;

@Service
public class ZaasClientService {

    private ZaasClient zaasClient;

    public ZaasClientService(ConfigProperties getConfigProperties) throws ZaasConfigurationException {
        zaasClient = new ZaasClientImpl(getConfigProperties);
    }

    public String login(String username, String password) throws ZaasClientException {
        return zaasClient.login(username, password);
    }

    public void logout(String token) throws ZaasConfigurationException, ZaasClientException, IOException {
        zaasClient.logout(token);
    }
}
