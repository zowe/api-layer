import { SEND_ERROR, CLEAR_ALL_ERRORS, GATEWAY_DOWN, GATEWAY_UP } from '../constants/error-constants';

const errorReducer = (state = { errors: [] }, action) => {
    switch (action.type) {
        case SEND_ERROR:
            return { ...state, errors: [...state.errors, action.payload] };

        case CLEAR_ALL_ERRORS:
            return { ...state, errors: [] };

        case GATEWAY_DOWN:
        case GATEWAY_UP:
            return { ...state, gatewayActive: action.payload };

        default:
            return state;
    }
};

export default errorReducer;
