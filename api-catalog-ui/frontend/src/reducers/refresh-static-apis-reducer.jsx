import { REFRESH_STATIC_APIS_SUCCESS, REFRESH_STATIC_APIS_ERROR } from '../constants/refresh-static-apis-constants';

const staticApisDefaultState = {
    shouldRefresh: false,
    error: null,
};

const refreshStaticApisReducer = (state = staticApisDefaultState, action) => {
    switch (action.type) {
        case REFRESH_STATIC_APIS_SUCCESS:
            return {
                ...state,
                shouldRefresh: action.shouldRefresh,
            };
        case REFRESH_STATIC_APIS_ERROR:
            return {
                ...state,
                error: action.error,
            };
        default:
            return state;
    }
};

export default refreshStaticApisReducer;
