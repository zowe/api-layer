/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.token;

import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;

public interface ZaasClient {

    void init(ConfigProperties configProperties);

    String login(String userId, String password) throws ZaasClientException;

    String login(String authorizationHeader) throws ZaasClientException;

    ZaasToken query(String token) throws ZaasClientException;

    String passTicket(String jwtToken, String applicationId) throws ZaasClientException;
}
