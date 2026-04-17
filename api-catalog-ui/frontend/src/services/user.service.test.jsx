/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { userService } from './user.service';

function mockFetch(data) {
    return jest.fn().mockImplementation(() =>
        Promise.resolve({
            ok: true,
            status: 200,
            text: () => Promise.resolve(JSON.stringify(data)),
            json: () => Promise.resolve(data),
        })
    );
}

describe('>>> User service tests', () => {
    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('should return user on login', async () => {
        const result = {};
        const fetchMock = mockFetch(result);
        global.fetch = fetchMock;
        const user = await userService.login({ username: 'user', password: 'password' });
        expect(user).toEqual(result);
        expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it('should logout', async () => {
        const result = {};
        const fetchMock = mockFetch(result);
        global.fetch = fetchMock;
        await userService.logout();
        expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it('should validate with query', async () => {
        const result = { status: 200 };
        const fetchMock = mockFetch(result);
        global.fetch = fetchMock;
        await userService.query();
        expect(fetchMock).toHaveBeenCalledTimes(1);
    });
});
