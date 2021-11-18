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
import history from '../helpers/history';

function login(credentials) {
    function request(user) {
        return { type: userConstants.USERS_LOGIN_REQUEST, user };
    }

    function success(user) {
        return { type: userConstants.USERS_LOGIN_SUCCESS, user };
    }

    function failure(error) {
        return { type: userConstants.USERS_LOGIN_FAILURE, error };
    }

    return (dispatch) => {
        dispatch(request(credentials));

        userService.login(credentials).then(
            (token) => {
                dispatch(success(token));
                history.push('/dashboard');
            },
            (error) => {
                dispatch(failure(error));
            }
        );
    };
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
                history.push('/login');
            },
            (error) => {
                dispatch(failure(error));
            }
        );
    };
}

const checkAuthenticated = (store) => (next) => (action) => {
    // eslint-disable-next-line
    console.log('checkAuthenticated');
    userService
        .checkAuthentication()
        .then(() => next(action))
        .catch((error) => {
            // eslint-disable-next-line
            console.log('checkAuthenticated error');
            // eslint-disable-next-line
            console.log(error);
            // eslint-disable-next-line
            console.log(window.location.href);
            if (!window.location.href.endsWith('/login')) {
                // eslint-disable-next-line
                console.log('checkAuthenticated catch block');
                history.push('/login');
                // return store.dispatch(history.push('/login'));
            }
            return next(action);
        });
};

// eslint-disable-next-line
export const userActions = {
    login,
    logout,
    checkAuthenticated,
};
