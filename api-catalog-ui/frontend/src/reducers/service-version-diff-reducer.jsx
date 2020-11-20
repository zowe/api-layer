import { REQUEST_VERSION_DIFF, RECEIVE_VERSION_DIFF } from "../actions/service-version-diff-actions";


const defaultState = {
    diffText: undefined
}

const serviceVersionDiffReducer = (state = defaultState, action) => {
    switch (action.type) {
        case REQUEST_VERSION_DIFF:
            //TODO:: Do we want to do anything on the request? we could set loading and show a loading indicator?
            return state;
        case RECEIVE_VERSION_DIFF:
            return {
                ...state,
                diffText: action.diffText
            }
        default:
            return state;
    }
};

export default serviceVersionDiffReducer;