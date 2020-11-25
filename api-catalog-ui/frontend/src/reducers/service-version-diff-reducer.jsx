import { REQUEST_VERSION_DIFF, RECEIVE_VERSION_DIFF } from "../actions/service-version-diff-actions";


const defaultState = {
    diffText: undefined,
    oldVersion: undefined,
    newVersion: undefined
}

const serviceVersionDiffReducer = (state = defaultState, action) => {
    switch (action.type) {
        case REQUEST_VERSION_DIFF:
            return {
                ...state,
                oldVersion: action.oldVersion,
                newVersion: action.newVersion
            }
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