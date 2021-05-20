import userConstants from '../constants/user-constants';

const sessionDefaultState = {
    sessionOn: false,
};

function authenticationReducer(state = sessionDefaultState, action = {}) {
    switch (action.type) {
        case userConstants.USERS_LOGIN_REQUEST:
            return {
                ...state,
                user: action.user,
            };
        case userConstants.USERS_LOGIN_SUCCESS:
            return {
                ...state,
                sessionOn: true,
                error: null,
                user: action.user,
                showHeader: true,
            };
        case userConstants.USERS_LOGIN_FAILURE:
            return {
                ...state,
                error: action.error,
            };
        case userConstants.AUTHENTICATION_FAILURE:
            return {
                ...state,
                error: action.error,
                sessionOn: sessionDefaultState.sessionOn,
                onCompleteHandling: () => {
                    sessionDefaultState.sessionOn = false;
                },
            };
        case userConstants.USERS_LOGOUT_REQUEST:
        case userConstants.USERS_LOGOUT_SUCCESS:
            return {
                ...state,
                error: null,
                showHeader: false,
                onCompleteHandling: () => {
                    sessionDefaultState.sessionOn = false;
                },
            };
        case userConstants.USERS_LOGOUT_FAILURE:
            return {
                ...state,
                error: action.error,
                showHeader: false,
            };
        default:
            return state;
    }
}

export default authenticationReducer;
