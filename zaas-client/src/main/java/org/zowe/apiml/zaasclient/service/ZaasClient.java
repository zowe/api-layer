/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaasclient.service;

import jakarta.servlet.http.HttpServletRequest;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;

/**
 * Get JWT tokens, PaasTickets and details about the Tokens.
 * Facade covering all operations related to the security API exposed via API Mediation Layer
 * The implementations hide the actual methods used to obtain the information.
 */
public interface ZaasClient {

    /**
     * Tries to login a user and create a new password given the id, the password of the old user and the new password. Both passwords are expected in plain text.
     *
     * @param userId   Id of the user as the user is known to the authentication service.
     * @param password Current password of the user which is passed through to the authentication service.
     * @param newPassword New password that the user wants to set up.
     * @return Valid JWT token obtained from the service
     * @throws ZaasClientException If the provided information were incorrect or some other issue with respect to the
     *                             communication with service occurs, this exception with details is thrown.
     *
     * @deprecated recommended to switch to {@link #login(String, char[], char[])} to handle cleaning up of passwords in memory
     */
    @Deprecated(forRemoval = false)
    String login(String userId, String password, String newPassword) throws ZaasClientException;

    /**
     * Tries to login a user given the id and the password of the user. The password is expected in plain text.
     *
     * @param userId   Id of the user as the user is known to the authentication service
     * @param password Password of the user which is passed through to the authentication service.
     * @return Valid JWT token obtained from the service
     * @throws ZaasClientException If the provided information were incorrect or some other issue with respect to the
     *                             communication with service occurs, this exception with details is thrown.
     *
     * @deprecated recommended to switch to {@link #login(String, char[])} to handle cleaning up of passwords in memory
     */
    @Deprecated(forRemoval = false)
    String login(String userId, String password) throws ZaasClientException;

    /**
     * Tries to login a user and create a new password given the id, the password of the old user and the new password. Both passwords are expected in plain text.
     *
     * @param userId   Id of the user as the user is known to the authentication service.
     * @param password Current password of the user which is passed through to the authentication service.
     * @param newPassword New password that the user wants to set up.
     * @return Valid JWT token obtained from the service
     * @throws ZaasClientException If the provided information were incorrect or some other issue with respect to the
     *                             communication with service occurs, this exception with details is thrown.
     */
    String login(String userId, char[] password, char[] newPassword) throws ZaasClientException;

    /**
     * Tries to login a user given the id and the password of the user. The password is expected in plain text.
     *
     * @param userId   Id of the user as the user is known to the authentication service
     * @param password Password of the user which is passed through to the authentication service.
     * @return Valid JWT token obtained from the service
     * @throws ZaasClientException If the provided information were incorrect or some other issue with respect to the
     *                             communication with service occurs, this exception with details is thrown.
     */
    String login(String userId, char[] password) throws ZaasClientException;

    /**
     * Tries to login a user given the information encoded in the format used by the Authorization HTTP header.
     *
     * @param authorizationHeader Base 64 encoded information retrievable e.g. from Authorization HTTP header
     * @return Valid JWT token obtained from service
     * @throws ZaasClientException If the provided information were incorrect or some other issue with respect to the
     *                             communication with service occurs, this exception with details is thrown.
     */
    String login(String authorizationHeader) throws ZaasClientException;

    /**
     * Return details about the provided JWT token. The token is expected to be valid.
     *
     * @param token JWT token to get more information about
     * @return Detailed information about the Token based on the information provided by ZAAS integrated in the API
     * Mediation Layer
     * @throws ZaasClientException If the provided token was expired, invalid or some other issue with respect to
     *                             communication occurs, this exception with details is thrown.
     */
    ZaasToken query(String token) throws ZaasClientException;

    /**
     * Return details about the JWT token in the provided HttpServletRequest. If no token exists a ZaasClientException
     * will be thrown.
     *
     * @param request HttpServletRequest representing the request with the token to be queried.
     * @return ZaasToken instance
     * @throws ZaasClientException thrown if no token is provided in the request, or if the provided token was expired,
     *                             invalid or some other issue with respect to communication occurs,
     *                             this exception with details is thrown.
     */
    ZaasToken query(HttpServletRequest request) throws ZaasClientException;

    /**
     * Retrieve PassTicket based on the valid JWT Token and the application id. To succeed the Application ID must
     * be known by the PassTicker provider and the JWT token must be valid and not expired.
     *
     * @param jwtToken      Valid JWT Token. One of the ways to obtain the token is via the login methods in this interface.
     * @param applicationId Valid application id known by the provider. The application Id will usually be up to 8 letters
     *                      long. Get the details from the PassTicket provider you are using.
     * @return Valid PassTicket to be used with other services supporting PassTicket as authorization method.
     * @throws ZaasClientException If the provided token was expired, invalid or application id was unknown or some other
     *                             issue with respect to communication occurs, this exception with details is thrown.
     */
    String passTicket(String jwtToken, String applicationId) throws ZaasClientException, ZaasConfigurationException;

    /**
     * Invalidate the provided JWT token in order to perform logout.
     *
     * @param token JWT token to invalidate
     * @throws ZaasClientException        If the provided token was expired, invalid or some other issue with respect to
     *                                    communication occurs, this exception with details is thrown.
     * @throws ZaasConfigurationException Wrapper for errors in HTTP client and TLS configuration.
     */
    void logout(String token) throws ZaasClientException, ZaasConfigurationException;
}
