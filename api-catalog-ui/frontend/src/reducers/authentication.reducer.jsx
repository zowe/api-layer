import userConstants from '../constants/user.constants';

function authenticationReducer(state = {}, action) {
    switch (action.type) {
        case userConstants.USERS_LOGIN_REQUEST:
            return {
                user: action.user,
            };
        case userConstants.USERS_LOGIN_SUCCESS:
            return {
                error: null,
                user: action.user,
                showHeader: true,
            };
        case userConstants.USERS_LOGIN_FAILURE:
            return {
                error: action.error,
            };
        case userConstants.AUTHENTICATION_FAILURE:
            return {
                error: action.error,
            };
        case userConstants.USERS_LOGOUT_REQUEST:
            return {
                error: null,
                showHeader: false,
            };
        case userConstants.USERS_LOGOUT_SUCCESS:
            return {
                error: null,
                showHeader: false,
            };
        case userConstants.USERS_LOGOUT_FAILURE:
            return {
                error: state.error,
                showHeader: false,
            };
        default:
            return state;
    }
}

export default authenticationReducer;
