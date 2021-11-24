import {userActions} from './user-actions';
import {userService} from '../services';
import userConstants from "../constants/user-constants";
import {act} from "react-dom/test-utils";

let dispatch;
let arr;
beforeEach(() => {
    arr = [];
    dispatch = jest.fn((x) => {
        arr.push(x.type)
    });
})
describe('User login actions', () => {

    let spy;

    beforeEach(() => {

        spy = jest.spyOn(userService, 'login');

    })
    it('notify on success', async () => {

        spy.mockReturnValue(new Promise((resolve) => {
            resolve('')
        }))

        await act(async () => userActions.login()(dispatch));

        expect(spy).toHaveBeenCalled();
        expect(arr[0]).toBe(userConstants.USERS_LOGIN_REQUEST);
        expect(arr[1]).toBe(userConstants.USERS_LOGIN_SUCCESS);
    })

    it('notify on error', async () => {
        const spy = jest.spyOn(userService, 'login');

        spy.mockReturnValue(new Promise((resolve, reject) => {
            reject('')
        }))

        await act(async () => userActions.login()(dispatch));

        expect(spy).toHaveBeenCalled();
        expect(arr[0]).toBe(userConstants.USERS_LOGIN_REQUEST);
        expect(arr[1]).toBe(userConstants.USERS_LOGIN_FAILURE);
    })
})

describe('User logout actions', () => {

    let spy;
    beforeEach(() => {

        spy = jest.spyOn(userService, 'logout');
    })
    it('notify logout on success', async () => {

        spy.mockReturnValue(new Promise((resolve) => {
            resolve('')
        }))

        await act(async () => userActions.logout()(dispatch));

        expect(spy).toHaveBeenCalled();
        expect(arr[0]).toBe(userConstants.USERS_LOGOUT_REQUEST);
        expect(arr[1]).toBe(userConstants.USERS_LOGOUT_SUCCESS);
    })

    it('notify logout on error', async () => {

        spy.mockReturnValue(new Promise((resolve, reject) => {
            reject('')
        }))

        await act(async () => userActions.logout()(dispatch));

        expect(spy).toHaveBeenCalled();
        expect(arr[0]).toBe(userConstants.USERS_LOGOUT_REQUEST);
        expect(arr[1]).toBe(userConstants.USERS_LOGOUT_FAILURE);
    })
})

describe('User actions failure', () => {

    it('notify about login failure', async () => {
        await act(async () => userActions.authenticationFailure('error')(dispatch));

        expect(arr[0]).toBe(userConstants.AUTHENTICATION_FAILURE);
    })

})
