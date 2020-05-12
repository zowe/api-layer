import { REFRESH_STATIC_APIS_SUCCESS, REFRESH_STATIC_APIS_ERROR } from '../constants/refresh-static-apis-constants';

const staticApisDefaultState = {
    refreshTimestamp: null,
    error: null,
};

const refreshStaticApisReducer = (state = staticApisDefaultState, action) => {
    switch (action.type) {
        case REFRESH_STATIC_APIS_SUCCESS:
            return {
                ...state,
                refreshTimestamp: action.refreshTimestamp,
                error: null,
            };
        case REFRESH_STATIC_APIS_ERROR:
            return {
                error: action.error,
            };
        default:
            return state;
    }
};

export default refreshStaticApisReducer;
