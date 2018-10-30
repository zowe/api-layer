const loadingReducer = (state = {}, action) => {
    const { type } = action;
    const matches = /(.*)_(REQUEST|SUCCESS|FAILURE|FAILED)/.exec(type);

    // not a *_REQUEST / *_SUCCESS /  *_FAILURE actions, so we ignore them
    if (!matches) {
        return state;
    }

    // Store whether a request is happening at the moment or not a *REQUEST action will be true
    const [, requestName, requestState] = matches;
    const isRequest = requestState === 'REQUEST';

    return {
        ...state,
        [requestName]: isRequest,
    };
};

export default loadingReducer;
