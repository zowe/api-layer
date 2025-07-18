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
import { userService } from '../services';

function login(credentials) {

    function request(user) {
        return { type: userConstants.USERS_LOGIN_REQUEST, user };
    }

    function success(user, showUpdatePassSuccess) {
        return { type: userConstants.USERS_LOGIN_SUCCESS, user, showUpdatePassSuccess };
    }

    function failure(error) {
        return { type: userConstants.USERS_LOGIN_FAILURE, error };
    }

    function invalidPassword(error) {
        return { type: userConstants.USERS_LOGIN_INVALIDPASSWORD, error };
    }
    function expiredPassword(error) {
        return { type: userConstants.USERS_LOGIN_EXPIREDPASSWORD, error };
    }
    return (dispatch) => {
        dispatch(request(credentials));

        userService.login(credentials).then(
            (token) => {
                let showUpdatePassSuccess = false;
                if (credentials.newPassword) {
                    showUpdatePassSuccess = true;
                }
                localStorage.setItem('username', credentials.username);
                dispatch(success(token, showUpdatePassSuccess));
            },
            (error) => {
                if (error.messageNumber === 'ZWEAT413E') {
                    dispatch(invalidPassword(error));
                } else if (error.messageNumber === 'ZWEAT412E') {
                    dispatch(expiredPassword(error));
                } else {
                    dispatch(failure(error));
                }
            }
        );
    };
}

export function forceLogout() {
    return (dispatch) => {
        dispatch({ type: userConstants.USERS_LOGOUT_SUCCESS });
    };
}

export function query(user) {
    const success = { showUpdatePassSuccess: true };
    return { type: userConstants.USERS_LOGIN_SUCCESS, user, success };
}

function logout() {
    function request() {
        return { type: userConstants.USERS_LOGOUT_REQUEST };
    }
    function success() {
        return { type: userConstants.USERS_LOGOUT_SUCCESS };
    }

    function failure(error) {
        return { type: userConstants.USERS_LOGOUT_FAILURE, error };
    }
    return (dispatch) => {
        dispatch(request());
        userService.logout().then(
            () => {
                dispatch(success());
                const channel = new BroadcastChannel('auth_channel');
                channel.postMessage('logout');
                channel.close();
            },
            (error) => {
                dispatch(failure(error));
            }
        );
    };
}

const authenticationFailure = (error) =>{
    function failure(err) {
        return { type: userConstants.AUTHENTICATION_FAILURE, error: err };
    }
    return (dispatch) => {
        dispatch(failure(error));
        if (error.xhr.getResponseHeader('WWW-Authenticate')) {
            window.location.href = process.env.REACT_APP_CATALOG_HOMEPAGE;
        }
    };
}

function returnToLogin() {
    function clean() {
        return { type: userConstants.USERS_LOGIN_INIT };
    }
    return (dispatch) => {
        dispatch(clean());
    };
}

function validateInput(credentials) {
    return { type: userConstants.USERS_LOGIN_VALIDATE, credentials };
}

function closeAlert() {
    return { type: userConstants.USERS_CLOSE_ALERT };
}

// eslint-disable-next-line
export const userActions = {
    login,
    logout,
    authenticationFailure,
    returnToLogin,
    validateInput,
    closeAlert,
    query,
    forceLogout,
};
