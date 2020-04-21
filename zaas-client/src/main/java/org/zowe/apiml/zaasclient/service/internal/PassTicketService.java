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

import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;

/**
 * Operations related to passTicket.
 */
interface PassTicketService {
    /**
     * Retrieve PassTicket based on the valid JWT Token and the application id. To succeed the Application ID must
     * be known by the PassTicker provider and the JWT token must be valid and not expired.
     *
     * @param jwtToken Valid JWT Token. One of the ways to obtain the token is via the login methods in this interface.
     * @param applicationId Valid application id known by the provider. The application Id will usually be up to 8 letters
     *                      long. Get the details from the PassTicket provider you are using.
     * @return Valid PassTicket to be used with other services supporting PassTicket as authorization method.
     * @throws ZaasClientException If the provided token was expired, invalid or application id was unknown or some other
     *      issue with respect to communication occurs, this exception with details is thrown.
     */
    String passTicket(String jwtToken, String applicationId) throws ZaasClientException, ZaasConfigurationException;
}
