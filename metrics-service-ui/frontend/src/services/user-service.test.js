/* eslint-disable no-undef */
import jest from 'jest-mock';
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

        const result = userService.login({ username: 'user', password: 'password' });
        expect(result).rejects.toThrow(errorMessage);
    });
});
