/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { act } from 'react-dom/test-utils';
import { userActions } from './user-actions';
import { userService } from '../services';
import userConstants from '../constants/user-constants';

let dispatch;
let arr;
beforeEach(() => {
    arr = [];
    dispatch = jest.fn((x) => {
        arr.push(x.type);
    });
});
describe('User login actions', () => {
    let spy;

    beforeEach(() => {
        spy = jest.spyOn(userService, 'login');
    });
    it('notify on success', async () => {
        spy.mockReturnValue(
            new Promise((resolve) => {
                resolve('');
            })
        );

        await act(async () => userActions.login()(dispatch));

        expect(spy).toHaveBeenCalled();
        expect(arr[0]).toBe(userConstants.USERS_LOGIN_REQUEST);
        expect(arr[1]).toBe(userConstants.USERS_LOGIN_SUCCESS);
    });

    it('notify on error', async () => {
        spy.mockReturnValue(
            new Promise((resolve, reject) => {
                reject(new Error(''));
            })
        );

        await act(async () => userActions.login()(dispatch));

        expect(spy).toHaveBeenCalled();
        expect(arr[0]).toBe(userConstants.USERS_LOGIN_REQUEST);
        expect(arr[1]).toBe(userConstants.USERS_LOGIN_FAILURE);
    });
});

describe('User logout actions', () => {
    let spy;
    beforeEach(() => {
        spy = jest.spyOn(userService, 'logout');
    });
    it('notify logout on success', async () => {
        spy.mockReturnValue(
            new Promise((resolve) => {
                resolve('');
            })
        );

        await act(async () => userActions.logout()(dispatch));

        expect(spy).toHaveBeenCalled();
        expect(arr[0]).toBe(userConstants.USERS_LOGOUT_REQUEST);
        expect(arr[1]).toBe(userConstants.USERS_LOGOUT_SUCCESS);
    });

    it('notify logout on error', async () => {
        spy.mockReturnValue(
            new Promise((resolve, reject) => {
                reject(new Error(''));
            })
        );

        await act(async () => userActions.logout()(dispatch));

        expect(spy).toHaveBeenCalled();
        expect(arr[0]).toBe(userConstants.USERS_LOGOUT_REQUEST);
        expect(arr[1]).toBe(userConstants.USERS_LOGOUT_FAILURE);
    });
});

describe('User actions failure', () => {
    it('notify about login failure', async () => {
        await act(async () => userActions.authenticationFailure('error')(dispatch));

        expect(arr[0]).toBe(userConstants.AUTHENTICATION_FAILURE);
    });
});
