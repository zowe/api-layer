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

import { jest } from '@jest/globals';
import { userService } from './user-service';

function mockFetch(data, resultOk = true) {
    return jest.fn().mockImplementation(() =>
        Promise.resolve({
            ok: resultOk,
            text: () => Promise.resolve(JSON.stringify(data)),
        })
    );
}

describe('>>> User service tests', () => {
    it('should return user on login', async () => {
        const result = {};
        global.fetch = mockFetch(result);

        const user = await userService.login({ username: 'user', password: 'password' });
        expect(user).toEqual(result);
        expect(fetch).toHaveBeenCalledTimes(1);
    });

    it('should return user on logout', async () => {
        global.fetch = mockFetch({});

        const result = await userService.logout({ username: 'user', password: 'password' });
        expect(result.ok).toBeTruthy();
        expect(fetch).toHaveBeenCalledTimes(1);
    });

    it('should reject login request if response is not ok', async () => {
        const errorMessage = 'my error';
        const data = { messages: [errorMessage] };
        global.fetch = mockFetch(data, false);

        await expect(userService.login({ username: 'user', password: 'password' })).rejects.toEqual(errorMessage);
    });
});
