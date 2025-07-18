/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import userConstants from '../constants/user-constants';

const initialState = {
    sessionOn: false,
    user: null,
    error: null,
    showHeader: false,
    loginSuccess: false,
    authenticationFailed: false,
    showUpdatePassSuccess: false,
    expired: false,
    expiredWarning: false,
    matches: false,
};

function authenticationReducer(state = initialState, action = {}) {
    switch (action.type) {
        case userConstants.USERS_LOGIN_REQUEST:
            return {
                ...state,
                user: action.user,
                authenticationFailed: false,
            };
        case userConstants.USERS_LOGIN_SUCCESS:
            initialState.sessionOn = true;
            return {
                ...state,
                error: null,
                user: action.user,
                showHeader: true,
                loginSuccess: true,
                authenticationFailed: false,
                showUpdatePassSuccess: action.showUpdatePassSuccess,
            };
        case userConstants.USERS_LOGIN_FAILURE:
            return {
                error: action.error,
            };
        case userConstants.AUTHENTICATION_FAILURE:
            return {
                error: action.error,
                authenticationFailed: true,
                sessionOn: initialState.sessionOn,
                onCompleteHandling: () => {
                    initialState.sessionOn = false;
                },
            };
        case userConstants.USERS_LOGIN_INVALIDPASSWORD:
            return {
                ...state,
                error: action.error,
                expiredWarning: false,
            };
        case userConstants.USERS_LOGIN_EXPIREDPASSWORD:
            return {
                ...state,
                error: action.error,
                expired: true,
                expiredWarning: true,
            };
        case userConstants.USERS_LOGIN_INIT:
            return {};
        case userConstants.USERS_LOGIN_VALIDATE:
            return {
                ...state,
                matches: action.credentials.newPassword === action.credentials.repeatNewPassword,
            };
        case userConstants.USERS_LOGOUT_REQUEST:
            return {
                ...state,
            };
        case userConstants.USERS_LOGOUT_SUCCESS:
            return {
                user: null,
                error: null,
                showHeader: false,
                loginSuccess: false,
                authenticationFailed: false,
                onCompleteHandling: () => {
                    initialState.sessionOn = false;
                },
            };
        case userConstants.USERS_LOGOUT_FAILURE:
            return {
                ...state,
                error: action.error,
                showHeader: false,
            };
        case userConstants.USERS_CLOSE_ALERT:
            return {
                ...state,
                showUpdatePassSuccess: false,
            };
        default:
            return state;
    }
}

export default authenticationReducer;
