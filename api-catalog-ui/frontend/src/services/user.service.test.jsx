/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import jest from 'jest-mock';
import { userService } from './user.service';

function mockFetch(data) {
    return jest.fn().mockImplementation(() =>
        Promise.resolve({
            ok: true,
            json: () => data,
        })
    );
}

describe('>>> User service tests', () => {
    it('should return user on login', async () => {
        const result = {};
        const fetch = mockFetch(result);
        const user = await userService.login({ username: 'user', password: 'password' });
        expect(user).toEqual(result);
        expect(fetch).toHaveBeenCalledTimes(1);
    });

    it('should logout', async () => {
        const result = {};
        const fetch = mockFetch(result);
        await userService.logout();
        expect(fetch).toHaveBeenCalledTimes(1);
    });
});
