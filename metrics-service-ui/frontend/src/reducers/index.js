import { combineReducers } from 'redux';
import filtersReducer from './filter-reducer';
import loadingReducer from './loading-reducer';
import authenticationReducer from './authentication-reducer';
import errorReducer from './error-reducer';

const reducers = {
    filtersReducer,
    loadingReducer,
    authenticationReducer,
    errorReducer,
};

// eslint-disable-next-line import/prefer-default-export
export const rootReducer = combineReducers(reducers);
