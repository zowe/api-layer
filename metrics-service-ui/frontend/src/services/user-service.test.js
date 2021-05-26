/* eslint-disable no-undef */
import jest from 'jest-mock';
import { userService } from './user-service';

function mockFetch(data) {
    return jest.fn().mockImplementation(() =>
        Promise.resolve({
            ok: true,
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
});
