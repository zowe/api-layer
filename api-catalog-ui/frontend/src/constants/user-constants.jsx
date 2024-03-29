/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

const userConstants = {
    USERS_LOGIN_REQUEST: 'USERS_LOGIN_REQUEST',
    USERS_LOGIN_SUCCESS: 'USERS_LOGIN_SUCCESS',
    USERS_LOGIN_FAILURE: 'USERS_LOGIN_FAILURE',
    AUTHENTICATION_FAILURE: 'AUTHENTICATION_FAILURE',
    USERS_LOGIN_INVALIDPASSWORD: 'USERS_LOGIN_INVALIDPASSWORD',
    USERS_LOGIN_EXPIREDPASSWORD: 'USERS_LOGIN_EXPIREDPASSWORD',
    USERS_LOGIN_INIT: 'USERS_LOGIN_INIT',
    USERS_LOGIN_VALIDATE: 'USERS_LOGIN_VALIDATE',

    USERS_LOGOUT_REQUEST: 'USERS_LOGOUT_REQUEST',
    USERS_LOGOUT_SUCCESS: 'USERS_LOGOUT_SUCCESS',
    USERS_LOGOUT_FAILURE: 'USERS_LOGOUT_FAILURE',
    USERS_CLOSE_ALERT: 'USERS_CLOSE_ALERT',
};

export default userConstants;
