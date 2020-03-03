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

    return dispatch => {
        dispatch(request(credentials));

        userService.login(credentials).then(
            token => {
                dispatch(success(token));
                history.push('/dashboard');
            },
            error => {
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
    return dispatch => {
        dispatch(request());
        userService.logout().then(
            () => {
                dispatch(success());
                history.push('/login');
            },
            error => {
                dispatch(failure(error));
            }
        );
    };
}

function authenticationFailure(error) {
    function failure(err) {
        return { type: userConstants.AUTHENTICATION_FAILURE, err };
    }
    return dispatch => {
        dispatch(failure(error));
        history.push('/login');
    };
}

// eslint-disable-next-line
export const userActions = {
    login,
    logout,
    authenticationFailure,
};
