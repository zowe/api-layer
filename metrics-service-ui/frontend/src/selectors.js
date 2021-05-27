import _ from 'lodash';

// check the current action against any states which are requests
// eslint-disable-next-line import/prefer-default-export
export const createLoadingSelector = (actions) => (state) =>
    _(actions).some((action) => _.get(state.loadingReducer, action));
