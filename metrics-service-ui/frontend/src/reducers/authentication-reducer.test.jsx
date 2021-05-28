/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/* eslint-disable no-undef */

import userConstants from '../constants/user-constants';
import authenticationReducer from './authentication-reducer';

it('should return default state in the default action', () => {
    expect(authenticationReducer()).toEqual({ sessionOn: false });
});

it('should handle USERS_LOGIN_REQUEST', () => {
    const action = {
        type: userConstants.USERS_LOGIN_REQUEST,
        user: 'user',
    };
    expect(authenticationReducer({}, action)).toEqual({ user: 'user' });
});

it('should handle USERS_LOGIN_SUCCESS', () => {
    const action = {
        type: userConstants.USERS_LOGIN_SUCCESS,
        user: 'user',
    };
    expect(authenticationReducer({}, action)).toEqual({ error: null, sessionOn: true, user: 'user' });
    expect(authenticationReducer()).toEqual({ sessionOn: true });
});

it('should handle USERS_LOGIN_FAILURE', () => {
    const action = {
        type: userConstants.USERS_LOGIN_FAILURE,
        error: 'error',
    };
    expect(authenticationReducer({}, action)).toEqual({ error: 'error' });
    expect(authenticationReducer()).toEqual({ sessionOn: true });
});

it('should handle AUTHENTICATION_FAILURE', () => {
    const action = {
        type: userConstants.AUTHENTICATION_FAILURE,
        error: 'error',
    };
    const result = authenticationReducer({}, action);
    expect(result.error).toEqual('error');
    expect(result.sessionOn).toEqual(true);
    expect(authenticationReducer()).toEqual({ sessionOn: true });
    result.onCompleteHandling();
    expect(authenticationReducer()).toEqual({ sessionOn: false });
});

it('should handle USERS_LOGOUT_REQUEST', () => {
    // Login again to recover from previous test case
    authenticationReducer({}, { type: userConstants.USERS_LOGIN_SUCCESS });

    const result = authenticationReducer({}, { type: userConstants.USERS_LOGOUT_REQUEST });
    expect(result.error).toEqual(null);
    expect(authenticationReducer()).toEqual({ sessionOn: true });
    result.onCompleteHandling();
    expect(authenticationReducer()).toEqual({ sessionOn: false });
});

it('should handle USERS_LOGOUT_SUCCESS', () => {
    // Login again to recover from previous test case
    authenticationReducer({}, { type: userConstants.USERS_LOGIN_SUCCESS });

    const result = authenticationReducer({}, { type: userConstants.USERS_LOGOUT_SUCCESS });
    expect(result.error).toEqual(null);
    expect(authenticationReducer()).toEqual({ sessionOn: true });
    result.onCompleteHandling();
    expect(authenticationReducer()).toEqual({ sessionOn: false });
});

it('should handle USERS_LOGOUT_FAILURE', () => {
    const action = {
        type: userConstants.USERS_LOGOUT_FAILURE,
        error: 'error',
    };
    expect(authenticationReducer({}, action)).toEqual({ error: 'error' });
    expect(authenticationReducer()).toEqual({ sessionOn: false });
});
